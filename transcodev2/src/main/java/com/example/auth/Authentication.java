package com.example.auth;

import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.core.json.JsonObject;
import com.example.db.DbJobs;

public class Authentication {

	DbJobs db = new DbJobs();

	public void generateToken(String username, String password, RoutingContext ctx, Vertx vertx,
			MongoClient mongoClient) {

		JWTAuthOptions config = new JWTAuthOptions()
				.setKeyStore(new KeyStoreOptions().setPath("keystore.jceks").setType("jceks").setPassword("secret"));

		JWTAuth provider = JWTAuth.create(vertx, config);

		db.verifyUser(mongoClient, username, password, res -> {
			if (res.succeeded()) {
				if (res.result() == true) {
					String token = provider.generateToken(new JsonObject().put("username", username),
							new JWTOptions().setExpiresInSeconds(3600));
					System.out.println(token);
					JsonObject tokenObj = new JsonObject().put("access_token", token).put("token_type", "bearer");
					ctx.json(tokenObj);
				} else {

					ctx.response().setStatusCode(401).end("invalid password");
				}
			} else {
				ctx.response ().setStatusCode(401).end(res.cause().getLocalizedMessage());
			}
		});

	}

	public void authenticateToken(String token, Vertx vertx, Handler<AsyncResult<JsonObject>> result) {

		JWTAuthOptions config = new JWTAuthOptions()
				.setKeyStore(new KeyStoreOptions().setPath("keystore.jceks").setType("jceks").setPassword("secret"));

		JWTAuth provider = JWTAuth.create(vertx, config);

		Credentials cred = new TokenCredentials(token);

		provider.authenticate(cred).onSuccess(user -> {
			System.out.println("User: " + user.principal());
			JsonObject resObj= new JsonObject().put("username", user.principal().getString("username")).put("Authenticated", "true");
			result.handle(Future.succeededFuture(resObj));
		}).onFailure(err -> {
			// Failed!
			result.handle(Future.failedFuture("invalid token"));
		});
	}
}
