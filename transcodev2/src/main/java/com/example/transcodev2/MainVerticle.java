package com.example.transcodev2;

import com.example.db.DbJobs;
import com.example.auth.Authentication;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends AbstractVerticle {

	@Override
	public void start() throws Exception {
		Router router = Router.router(vertx);

		String dbURL = "mongodb+srv://mongouser:pass123@cluster0.f2jbzrt.mongodb.net/?retryWrites=true&w=majority";
		JsonObject config = new JsonObject().put("connection_string", dbURL).put("db_name", "transcodeDB")
				.put("connectTimeoutMS", 5000).put("socketTimeoutMS", 3000);
		MongoClient mongoClient = MongoClient.createShared(vertx, config);

		// Authentication
		Authentication auth = new Authentication();

		// DB Connection
		DbJobs db = new DbJobs();
		db.getDbConnection(mongoClient);

		// Authorization
		router.route("/transcodev1/api/*").handler(ctx -> {
			String token = ctx.request().headers().get("Authorization");
			System.out.println(token);
			if (token == null) {
				ctx.response().setStatusCode(401).end();
			} else {

				auth.authenticateToken(token, vertx, res -> {
					if (res.succeeded()) {
						System.out.println("username: " + res.result().getString("username"));
						ctx.next();
					} else {
						System.out.println(res.cause().getLocalizedMessage());
						ctx.response().setStatusCode(401).end(res.cause().getLocalizedMessage());
					}
				});
			}
		});

		// Post - CreateJob
		router.post("/transcodev1/api/jobs").handler(BodyHandler.create()).handler(ctx -> {
			db.createJob(mongoClient, ctx);
		});

		// start the job
		router.post("/transcodev1/api/jobs/:jobid/start").handler(ctx -> {
			String jobId = ctx.pathParam("jobid");
			db.startJob(mongoClient, ctx, jobId, vertx);
		});

		// stop the job
		router.post("/transcodev1/api/jobs/:jobid/stop").handler(ctx -> {
			String jobId = ctx.pathParam("jobid");
			System.out.println("Stopping job " + jobId);
			db.stopJob(mongoClient, ctx, jobId);

		});

		// delete the job
		router.delete("/transcodev1/api/jobs/:jobid").handler(ctx -> {
			String jobId = ctx.pathParam("jobid");
			db.deleteJob(mongoClient, ctx, jobId);
		});

		// generate token
		router.post("/transcodev1/users/token").handler(BodyHandler.create()).handler(ctx -> {
			RequestBody reqObj = ctx.body();
			JsonObject reqJSON = reqObj.asJsonObject();
			System.out.println(reqJSON);
			String username = reqJSON.getString("username");
			String password = reqJSON.getString("password");
			if (username == null || password == null) {
				ctx.response().setStatusCode(401).end("username or password cannot be empty");
			} else {

				auth.generateToken(username, password, ctx, vertx, mongoClient);
			}
		});

		router.post("/transcodev1/users/authenticate").handler(BodyHandler.create()).handler(ctx -> {
			RequestBody reqObj = ctx.body();
			JsonObject reqJSON = reqObj.asJsonObject();
			System.out.println(reqJSON);
			auth.authenticateToken(reqJSON.getString("token"), vertx, res -> {
				if (res.succeeded()) {
					System.out.println("username: " + res.result().getString("username"));
					ctx.json(res.result());
				} else {
					ctx.response().setStatusCode(401).end();
				}
			});
		});

		// get all jobs
		router.get("/transcodev1/api/jobs").produces("*/json").handler(ctx -> {
			db.getAllJobs(mongoClient, ctx);
		});

		// find job by id
		router.get("/transcodev1/api/jobs/:id").produces("*/json").handler(ctx -> {
			String id = ctx.request().getParam("id");
			db.findById(mongoClient, ctx, id);
		});

		vertx.createHttpServer().requestHandler(router).listen(8888)
				.onSuccess(server -> System.out.println("HTTP server started on port " + server.actualPort()));
	}
}
