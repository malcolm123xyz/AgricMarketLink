package mx.moble.solution.backend.responses;

import mx.moble.solution.backend.dataModel.LoginData;


public class SignUpLoginResponse {

    private String response = "DONE";
    private int returnCode = ResponseCodes.OK;
    private LoginData loginData;

    public SignUpLoginResponse(LoginData loginData){
        this.loginData = loginData;
    }

    public SignUpLoginResponse(String response, int errorCode){
        this.response = response;
        this.returnCode = errorCode;
    }

    public LoginData getLoginData() {
        return loginData;
    }

    public void setLoginData(LoginData loginData) {
        this.loginData = loginData;
    }

    public String getResponse() {
        return response;
    }

    public int getReturnCode() {
        return returnCode;
    }
}
