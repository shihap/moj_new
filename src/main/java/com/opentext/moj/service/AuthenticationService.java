package com.opentext.moj.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthenticationService {
    @Value("${BASE_URL}")
    private String BASE_URL;

    /**
     * user authentication
     * @param username username of content server
     * @param password password of content server
     * @return otcs ticket
     */
    public JSONObject authenticate(String username, String password) {
        JSONObject result = new JSONObject();
        try {
            String url = BASE_URL + "v1/auth";

            LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("username", username);
            map.add("password", password);
            HttpEntity<LinkedMultiValueMap<String,String>> reqEntity = new HttpEntity<LinkedMultiValueMap<String,String>>(map);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, reqEntity, String.class);

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                return new JSONObject(responseEntity.getBody());
            } else {
                result.put("error", "Invalid username/password specified.");
                return result;
            }
        } catch (HttpClientErrorException.BadRequest
                 | HttpClientErrorException.Unauthorized e) {
            result.put("error", "Invalid username/password specified.");
            return result;
        } catch (HttpServerErrorException.InternalServerError e) {
            result.put("error", "Argument \"username\"/\"password\" is required.");
            return result;
        }
    }
}
