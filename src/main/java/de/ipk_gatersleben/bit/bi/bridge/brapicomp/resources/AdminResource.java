package de.ipk_gatersleben.bit.bi.bridge.brapicomp.resources;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.glassfish.jersey.process.internal.RequestScoped;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.j256.ormlite.dao.Dao;

import de.ipk_gatersleben.bit.bi.bridge.brapicomp.Config;
import de.ipk_gatersleben.bit.bi.bridge.brapicomp.ci.EmailManager;
import de.ipk_gatersleben.bit.bi.bridge.brapicomp.dbentities.Endpoint;
import de.ipk_gatersleben.bit.bi.bridge.brapicomp.dbentities.EndpointService;
import de.ipk_gatersleben.bit.bi.bridge.brapicomp.dbentities.TestReport;
import de.ipk_gatersleben.bit.bi.bridge.brapicomp.dbentities.TestReportService;
import de.ipk_gatersleben.bit.bi.bridge.brapicomp.testing.config.TestCollection;
import de.ipk_gatersleben.bit.bi.bridge.brapicomp.testing.reports.TestSuiteReport;
import de.ipk_gatersleben.bit.bi.bridge.brapicomp.utils.DataSourceManager;
import de.ipk_gatersleben.bit.bi.bridge.brapicomp.utils.JsonMessageManager;
import de.ipk_gatersleben.bit.bi.bridge.brapicomp.utils.ResourceService;
import de.ipk_gatersleben.bit.bi.bridge.brapicomp.utils.RunnerService;

/**
 * Currently groups the Administrator related queries.
 * Mostly running tests on endpoints stored in the database.
 * Error codes: X0XX
 */
@Path("/admin")
@RequestScoped
public class AdminResource {

    private static final Logger LOGGER = LogManager.getLogger(AdminResource.class.getName());

    //Commented out for security. Also private API key is required
    /*
    *//**
     * Run the default test on all endpoints
     *
     * @return Response with json report
     *//*
    @POST
    @Path("/testall")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generalTest(@Context HttpHeaders headers, @QueryParam("frequency") String frequency) {

        LOGGER.debug("New POST /testall call.");
        try {

            String[] auth = ResourceService.getAuth(headers);
            //Check auth header
            if (auth == null || auth.length != 2) {
                String e = JsonMessageManager.jsonMessage(401, "unauthorized", 4000);
                return Response.status(Status.UNAUTHORIZED).entity(e).build();
            }

            //Check if api key is correct.
            if (!auth[1].equals(Config.get("adminkey"))) {
                String e = JsonMessageManager.jsonMessage(403, "missing or wrong apikey", 4001);
                return Response.status(Status.UNAUTHORIZED).entity(e).build();
            }

            ObjectMapper mapper = new ObjectMapper();

            InputStream inJson = TestCollection.class.getResourceAsStream("/collections/CompleteBrapiTest.custom_collection.json");
            TestCollection tc = mapper.readValue(inJson, TestCollection.class);

            int count = RunnerService.TestAllEndpointsWithFreq(tc, frequency);
            boolean success = false;
            if (count > 0) {
                success = true;
            }
            return Response.status(Status.ACCEPTED).entity(success).build();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            String e1 = JsonMessageManager.jsonMessage(500, "internal server error", 5001);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e1).build();
        }
    }

    @DELETE
    @Path("/tables")
    public Response delTables(@Context HttpHeaders headers) {
    	// It is necessary to delete or update the database tables after the schema changes
    	// i.e. when a new parameter/column is added
        try {
        	
        	String[] auth = ResourceService.getAuth(headers);
            //Check auth header
            if (auth == null || auth.length != 2) {
                String e = JsonMessageManager.jsonMessage(401, "unauthorized", 4000);
                return Response.status(Status.UNAUTHORIZED).entity(e).build();
            }

            //Check if api key is correct.
            if (!auth[1].equals(Config.get("adminkey"))) {
                String e = JsonMessageManager.jsonMessage(403, "missing or wrong apikey", 4001);
                return Response.status(Status.UNAUTHORIZED).entity(e).build();
            }
            
            DataSourceManager.deleteTable(TestReport.class);
            DataSourceManager.deleteTable(Endpoint.class);
        } catch (SQLException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
        return Response.ok().build();
    }
    @GET
    @Path("/users")
    public Response getUsers(@Context HttpHeaders headers) {
    	// It is necessary to delete or update the database tables after the schema changes
    	// i.e. when a new parameter/column is added
    	List<Endpoint> l;
        try {
        	
        	String[] auth = ResourceService.getAuth(headers);
            //Check auth header
            if (auth == null || auth.length != 2) {
                String e = JsonMessageManager.jsonMessage(401, "unauthorized", 4000);
                return Response.status(Status.UNAUTHORIZED).entity(e).build();
            }

            //Check if api key is correct.
            if (!auth[1].equals(Config.get("adminkey"))) {
                String e = JsonMessageManager.jsonMessage(403, "missing or wrong apikey", 4001);
                return Response.status(Status.UNAUTHORIZED).entity(e).build();
            }
        	
            l = EndpointService.getAllEndpoints();
            
        } catch (SQLException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
        return Response.ok().entity(l.toString()).build();
    }*/
    
    /**
     * Register new public endpoint
     *
     * @param endp Json containing the endpoint's url.
     * @return Json message.
     */
    @POST
    @Path("/endpoints")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createEndpoint(@Context HttpHeaders headers,
                                   Endpoint endp) {

        LOGGER.debug("New POST /admin/endpoints call.");
        
        Dao<Endpoint, UUID> endpointDao = DataSourceManager.getDao(Endpoint.class);

        try {
        	// Check if the record exists in the database already.
            Endpoint e = EndpointService.getEndpointWithEmailAndUrlAndFreq(endp.getEmail(), endp.getUrl(), endp.getFrequency());
            if (e != null && e.isConfirmed()) {
                String e2 = JsonMessageManager.jsonMessage(400, "Url already in use", 4002);
                return Response.status(Status.BAD_REQUEST).entity(e2).build();
            } else {
            	endp.setEmail(null);
            	endp.setPublic(true);
                endpointDao.create(endp);
                InputStream inJson = TestCollection.class.getResourceAsStream("/collections/CompleteBrapiTest.custom_collection.json");
                TestCollection tc;
                ObjectMapper mapper = new ObjectMapper();
         		tc = mapper.readValue(inJson, TestCollection.class);
                RunnerService.TestEndpointWithCallAndSaveReport(endp, tc);
                return Response.status(Status.ACCEPTED).entity(JsonMessageManager.jsonMessage(200, "Public endpoint added.", 2101)).build();
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            String e1 = JsonMessageManager.jsonMessage(500, "Internal server error", 5002);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e1).build();
        }
    }
}
