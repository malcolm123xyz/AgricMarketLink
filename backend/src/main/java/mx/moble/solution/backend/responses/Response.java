package mx.moble.solution.backend.responses;

import java.util.List;

import mx.moble.solution.backend.dataModel.DatabaseObject;


public class Response {

    private final String response;
    private final int returnCode;

    public Response (){
        response = "";
        returnCode = -1;
    }

    public Response(String msg, int code){
        response = msg;
        returnCode = code;
    }

    public static Response OK(){
        return new Response("Done", ResponseCodes.OK);
    }

    public String getResponse() {
        return response;
    }

    public int getReturnCode() {
        return returnCode;
    }
}
