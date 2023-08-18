package com.example.db;

import com.example.notify.Notification;


import java.util.List;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;

public class DbJobs {
	
	Notification notify = new Notification();

	public void getDbConnection(MongoClient mongoClient) {
		mongoClient.getCollections(res -> {
			if (res.succeeded()) {
				List<String> collections = res.result();
				System.out.println(collections);
				System.out.println("DB connection successful");
			} else {
				res.cause().printStackTrace();
			}
		});
	}

	public void verifyUser(MongoClient mongoClient, String username, String password,
			Handler<AsyncResult<Boolean>> result) {

		JsonObject query = new JsonObject().put("_id", username);

		mongoClient.findOne("userdata", query, null, res -> {
			if (res.succeeded()) {
				if (res.result() != null) {
					JsonObject resObj = res.result();
					if (resObj.getString("password").equals(password)) {
						result.handle(Future.succeededFuture(true));
					} else {
						result.handle(Future.succeededFuture(false));
					}
				} else {
					result.handle(Future.failedFuture("user not found"));
				}
			} else {
				result.handle(Future.failedFuture("user not found"));
				res.cause().printStackTrace();
			}
		});
	}

	public void createJob(MongoClient mongoClient, RoutingContext ctx) {
		RequestBody reqObj = ctx.body();
		JsonObject reqJSON = reqObj.asJsonObject();
		if (reqJSON == null) {
			ctx.response().setStatusCode(400).end("request body cannot be empty");
		}
		JsonObject document = new JsonObject().put("data", reqJSON).put("job-status", "ready");
		JsonObject resJSON = new JsonObject();
		mongoClient.insert("jobsdata", document, res -> {
			if (res.succeeded()) {
				String id = res.result();
				System.out.println("Created job with id " + id);
				resJSON.put("id", id);
//				notify.sendToHub(id, "job_created");
				ctx.response().putHeader("Location", "./" + id);
				ctx.json(resJSON);
			} else {
				res.cause().printStackTrace();
			}
		});
	}

	public void getAllJobs(MongoClient mongoClient, RoutingContext ctx) {
		JsonObject document = new JsonObject();
		mongoClient.find("jobsdata", document, res -> {
			if (res.succeeded()) {
				List<JsonObject> list = res.result();
				ctx.response().end(Json.encodePrettily(list));
			}
		});
	}

	public void findById(MongoClient mongoClient, RoutingContext ctx, String id) {
		JsonObject document = new JsonObject().put("_id", id);
		if (id == null) {
			ctx.response().end("Id is null, please provide correct id");
		} else {
			mongoClient.find("jobsdata", document, res -> {
				if (res.succeeded()) {
					List<JsonObject> resJson = res.result();
					if (resJson.size() == 0) {
						ctx.response().end("Job not found");
					} else {
						for (JsonObject json : res.result()) {
							ctx.response().end(Json.encodePrettily(json));
						}
					}
				} else {
					System.out.println("db call failed");
					ctx.response().end("please provide correct id");
					res.cause().printStackTrace();
				}
			});
		}
	}

	public void startJob(MongoClient mongoClient, RoutingContext ctx, String jobId, Vertx vertx) {
		System.out.println("Starting job " + jobId);
		JsonObject query = new JsonObject().put("_id", jobId);
		JsonObject update = new JsonObject().put("$set", new JsonObject().put("job-status", "running"));
		mongoClient.findOne("jobsdata", query, null, r -> {
			if (r.succeeded()) {
				JsonObject respObj = r.result();
				if (respObj == null) {
					ctx.response().end("Job not found");
				} else {

					if (respObj.getString("job-status").equals("running")) {
						System.out.println("Job is already running");
						ctx.response().end("Job is already running");
					} else {
						mongoClient.updateCollection("jobsdata", query, update, res -> {
							if (res.succeeded()) {
								System.out.println("Job status updated !");
								ctx.response().end("job started");
							} else {
								res.cause().printStackTrace();
							}
						});

						WorkerExecutor executor = vertx.createSharedWorkerExecutor("my-worker-pool");

						executor.executeBlocking(l -> {
							vertx.setTimer(12000, i -> {

								mongoClient.findOne("jobsdata", query, null, resp -> {
									if (resp.succeeded()) {
										JsonObject resObj = resp.result();
										System.out.println(resObj);
										if (resObj.getString("job-status").equals("running")) {
											JsonObject u = new JsonObject().put("$set",
													new JsonObject().put("job-status", "completed"));
											mongoClient.updateCollection("jobsdata", query, u, res -> {
												if (res.succeeded()) {
													System.out.println("Job completed");
												} else {
													res.cause().printStackTrace();
												}
											});
										} else {
											System.out.println("Job status " + resObj.getString("job-status"));
										}
									} else {
										resp.cause().printStackTrace();
									}
								});

							});
						}, false, res -> {
							if (res.succeeded()) {
								System.out.println("exec completed");
							}
						});

					}
				}
			}
		});
	}

	public void stopJob(MongoClient mongoClient, RoutingContext ctx, String jobId) {
		JsonObject query = new JsonObject().put("_id", jobId);
		JsonObject update = new JsonObject().put("$set", new JsonObject().put("job-status", "aborted"));

		mongoClient.findOne("jobsdata", query, null, res -> {
			if (res.succeeded()) {
				JsonObject resObj = res.result();
				System.out.println(resObj);
				if (resObj == null) {
					ctx.response().end("Job not found");
				} else {
					if (resObj.getString("job-status").equals("running")) {
						mongoClient.updateCollection("jobsdata", query, update, resp -> {
							if (resp.succeeded()) {
								System.out.println("Job status updated !");
								ctx.response().end("job aborted");
							} else {
								resp.cause().printStackTrace();
							}
						});
					} else {
						System.out.println("Job is not running, failed to abort.");
						ctx.response().end("Job is not running, failed to abort.");
					}
				}

			} else {
				res.cause().printStackTrace();
				ctx.fail(500);
				ctx.end("Internal server error");
			}

		});
	}

	public void deleteJob(MongoClient mongoClient, RoutingContext ctx, String jobId) {
		System.out.println("Deleting job " + jobId);
		JsonObject query = new JsonObject().put("_id", jobId);

		mongoClient.removeDocument("jobsdata", query, res -> {
			if (res.succeeded()) {
				System.out.println("Deleted job successfully");
				ctx.response().end("Job deleted successfully");
			} else {
				res.cause().printStackTrace();
				ctx.response().end("Job not deleted");
			}
		});
	}

}
