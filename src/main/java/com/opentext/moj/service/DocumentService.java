package com.opentext.moj.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;

@Service
public class DocumentService {

    @Value("${BASE_URL}")
    private String BASE_URL;

    @Value("${WEB_REPORT_ID}")
    private String WEB_REPORT_ID;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private CategoryService categoryService;

    /**
     * create document with category
     * @param file the file will be uploaded
     * @param catName category name will be added to the document
     * @param catValues category values
     * @param parentPath path of the destination folder
     * @param username content server username
     * @param password content server user password
     * @return the id of created document
     */
    public ResponseEntity<?> createDocument(MultipartFile file, String catName, String catValues, String parentPath, String username, String password) {
        JSONObject result = new JSONObject();
        try {
            String fileName = file.getOriginalFilename();
            JSONObject token = authenticationService.authenticate(username, password);
            if (token.has("error")) {
                return new ResponseEntity<>("401 Unauthorized (username or password incorrect )", HttpStatus.UNAUTHORIZED);
            }
            //get the parentId that file will be uploaded in
            JSONObject getParentResult = getParentId(parentPath, token.getString("ticket"));

            if (getParentResult.has("error")) {
                return new ResponseEntity<>(getParentResult.get("error"), HttpStatus.BAD_REQUEST);
            }

            String url = BASE_URL + "v1/nodes";

            HttpHeaders headers = new HttpHeaders();
            headers.add("otcsticket", token.getString("ticket"));

            MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();

            requestBody.add("file", convertMultipartFileToByteArrayResource(file));

            // construct request body parameter
            JSONObject bodyParameter = new JSONObject();

            // adding file type
            bodyParameter.put("type", 144);

            // adding the file name
            bodyParameter.put("name", fileName);

            // construct categories body
            JSONObject categoryJson = constructUploadDocumentBody(catName, catValues);
            if (categoryJson.has("error")) {
                return new ResponseEntity<>(categoryJson.toString(), HttpStatus.BAD_REQUEST);
            }
            bodyParameter.put("roles", categoryJson);

            bodyParameter.put("parent_id", getParentResult.getString("parent_id"));

            requestBody.add("body", bodyParameter.toString());

            HttpEntity<MultiValueMap<String, Object>> reqEntity = new HttpEntity<>(requestBody, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, reqEntity, String.class);

            System.out.println(response.getBody());
            System.out.println(new JSONObject(response.getBody()).get("id"));

            String id = new JSONObject(response.getBody()).getString("id");

            return new ResponseEntity<>("SUCCESS DOCUMENT ID IS: " + id, HttpStatus.CREATED);
        } catch (HttpClientErrorException.BadRequest e) {
            //result.put("error", e.getMessage());
            String errMsg = e.getMessage().substring(17);
            String finalMsg = errMsg.substring(1, errMsg.length() - 1);
            return new ResponseEntity<>(finalMsg, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    /**
     * get document by id and version number
     * @param nodeId document id
     * @param versionNumber document version
     * @param username content server username
     * @param password content server user password
     * @return byte[] for the specified document
     */
    public ResponseEntity<?> getDocumentById(String nodeId, String versionNumber, String username, String password) throws Exception {
        JSONObject result = new JSONObject();
        try {
            JSONObject token = authenticationService.authenticate(username, password);
            if(token.has("error")) {
                return new ResponseEntity<>("401 Unauthorized (username or password incorrect)", HttpStatus.UNAUTHORIZED);
            }

            JSONObject getDocumentNameResult = getDocumentName(nodeId, token.getString("ticket"));

            if(getDocumentNameResult.has("error")) {
                return new ResponseEntity<>(getDocumentNameResult.get("error"), HttpStatus.NOT_FOUND);
            }

            String url = BASE_URL + "v1/nodes/" + nodeId + "/versions/" + versionNumber + "/content";

            HttpHeaders headers = new HttpHeaders();
            headers.add("otcsticket", token.getString("ticket"));

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

            HttpEntity<MultiValueMap<String, String>> reqEntity = new HttpEntity<MultiValueMap<String, String>>(body,
                    headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<byte []> response = restTemplate.exchange(url, HttpMethod.GET, reqEntity, byte[].class);

            ArrayList<String> resultArr = new ArrayList<>();
            resultArr.add("Document_Content:".concat(Base64.getEncoder().encodeToString(response.getBody())));
            resultArr.add("Document_Name:".concat(getDocumentNameResult.getString("name")));

//            result.put("Document_Content", Base64.getEncoder().encodeToString(response.getBody()));
//            result.put("Document_Name", getDocumentNameResult.get("name"));
            return new ResponseEntity<>(resultArr, HttpStatus.OK);

        } catch (HttpClientErrorException.BadRequest | HttpServerErrorException.InternalServerError e) {
            result.put("error", "Document with id = " + nodeId + " or version_no = " + versionNumber + " is not exist");
            return new ResponseEntity<>("The version is not found.", HttpStatus.NOT_FOUND);
        } catch (HttpClientErrorException.Unauthorized e) {
            result.put("error", "Unauthorized");
            return new ResponseEntity<>(result.toString(), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public JSONObject getDocumentName(String nodeId, String ticket) {
        JSONObject result = new JSONObject();
        try {
            String url = BASE_URL + "v1/nodes/" + nodeId + "?fields=data";
            HttpHeaders headers = new HttpHeaders();
            headers.add("otcsticket",ticket);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

            HttpEntity<MultiValueMap<String, String>> reqEntity = new HttpEntity<MultiValueMap<String, String>>(body,
                    headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, reqEntity, String.class);

            result.put("name", new JSONObject(response.getBody()).getJSONObject("data").getString("name"));
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
            result.put("error", "404 Document Not Found");
        } catch (Exception e) {
            result.put("error", "INTERNAL SERVER ERROR");
        }
        return result;
    }

    public JSONObject getDocumentVersions(String nodeId,String versionNumber, String ticket) {
        JSONObject result = new JSONObject();
        try {
            String url = BASE_URL + "v2/nodes/" + nodeId + "?fields=versions";
            HttpHeaders headers = new HttpHeaders();
            headers.add("otcsticket",ticket);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

            HttpEntity<MultiValueMap<String, String>> reqEntity = new HttpEntity<MultiValueMap<String, String>>(body,
                    headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, reqEntity, String.class);

            JSONArray versionsArr = new JSONObject(response.getBody())
                    .getJSONObject("results")
                    .getJSONObject("data")
                    .getJSONArray("versions");

            boolean versionExist = false;
            for(int i = 0; i < versionsArr.length(); i++) {
                JSONObject version = versionsArr.getJSONObject(i);
                if(version.getString("version_number_name").equals(versionNumber)) {
                    versionExist = true;
                    break;
                }
            }

            if(versionNumber.equals("0")) {
                return result.put("versions", 0);
            }

            if (!versionExist) {
                result.put("error", "Version dosen't exist");
                return result;
            }
            result.put("versions", versionsArr.length());
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
            result.put("error", "Document dosen't exist");
        } catch (Exception e) {
            result.put("error", "INTERNAL SERVER ERROR");
        }
        return result;
    }

    /**
     * delete document by id
     * @param nodeId document id
     * @param username content server username
     * @param password content server user password
     * @return object for the action status
     */
    public ResponseEntity<?> deleteDocumentById(String nodeId,String versionNumber, String username, String password) throws Exception {
        JSONObject result = new JSONObject();
        try {
            JSONObject token = authenticationService.authenticate(username, password);
            if(token.has("error")) {
                return new ResponseEntity<>("INVALID USERNAME OR PASSWORD", HttpStatus.UNAUTHORIZED);
            }
            JSONObject getVersionsResult = getDocumentVersions(nodeId, versionNumber, token.getString("ticket"));

            if(getVersionsResult.has("error")) {
                return new ResponseEntity<>(getVersionsResult.get("error"), HttpStatus.BAD_REQUEST);
            }
            String url = BASE_URL;

            if(getVersionsResult.getInt("versions") > 1) {
                 url += "v1/nodes/" + nodeId + "/versions/" + versionNumber;
            }
            else {
                url += "v1/nodes/" + nodeId;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add("otcsticket", token.getString("ticket"));

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

            HttpEntity<MultiValueMap<String, String>> reqEntity = new HttpEntity<MultiValueMap<String, String>>(body,
                    headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, reqEntity, String.class);

            if(versionNumber.equals("0")) {
                result.put("result", "document with id: " + nodeId + " is deleted successfully");
            } else {
                result.put("result", "version number: " + versionNumber + " is deleted successfully");
            }
            return new ResponseEntity<>("DELETED", HttpStatus.OK);

        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
            result.put("error", "No document found with id = " + nodeId + " or version_no = " + versionNumber);
            return new ResponseEntity<>("Document dosen't exist", HttpStatus.NOT_FOUND);
        }
        catch (Exception e) {
            result.put("error", e.getMessage());
            return new ResponseEntity<>("INTERNAL SERVER ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * import new version of a document
     * @param nodeId document id
     * @param file the document that will be the new version
     * @param username content server username
     * @param password content server user password
     * @return the document id and version number
     */
    public ResponseEntity<?> importVersionById(String nodeId, MultipartFile file, String username, String password) {
        JSONObject result = new JSONObject();
        try {
            JSONObject token = authenticationService.authenticate(username, password);
            if(token.has("error")) {
                return new ResponseEntity<>("401 Unauthorized (username or password incorrect)", HttpStatus.UNAUTHORIZED);
            }

            String url = BASE_URL + "v1/nodes/" + nodeId + "/versions";

            HttpHeaders headers = new HttpHeaders();
            headers.add("otcsticket", token.getString("ticket"));

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", convertMultipartFileToByteArrayResource(file));

            HttpEntity<MultiValueMap<String, Object>> reqEntity = new HttpEntity<MultiValueMap<String, Object>>(body,
                    headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, reqEntity, String.class);

//            result.put("version_number", new JSONObject(response.getBody()).get("version_number"));
//            result.put("message", "new version is added successfully");
            return new ResponseEntity<>("SUCCESS DOCUMENT VERSION IS:" + new JSONObject(response.getBody()).get("version_number"), HttpStatus.CREATED);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
            result.put("error", "the document with id = " + nodeId + " could not be accessed. Either it does not exist or you do not have permission to access it.");
            return new ResponseEntity<>("404 Document dosen't exist ", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result.toString(), HttpStatus.INTERNAL_SERVER_ERROR);        }
    }

    /**
     * convert from multipart file to byteArrayResource
     * @param multipartFile document
     * @return document as byteArrayResource
     */
    public ByteArrayResource convertMultipartFileToByteArrayResource(MultipartFile multipartFile) {
        try {
            byte[] fileBytes = multipartFile.getBytes();
            return new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return multipartFile.getOriginalFilename();
                }
            };
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * get the folder node id for the document will be added in
     * @param parentPath full path of the document
     * @return node id
     */
    public JSONObject getParentId(String parentPath, String ticket) {
        JSONObject result = new JSONObject();
        try {
            String url = BASE_URL + "v1/nodes/" + WEB_REPORT_ID + "/output?FolderPath=" + parentPath;
            HttpHeaders headers = new HttpHeaders();
            headers.add("otcsticket",ticket);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

            HttpEntity<MultiValueMap<String, String>> reqEntity = new HttpEntity<MultiValueMap<String, String>>(body,
                    headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, reqEntity, String.class);

            String parentId = new JSONObject(response.getBody()).getString("data");
            if(!isNumeric(parentId)) {
                result.put("error", "400 Path Not Found");
            }
            result.put("parent_id", parentId);
        } catch (Exception e) {
            result.put("error", "400 Path Not Found");
        }
        return result;
    }

    /**
     * construct the createDocument request body
     * @param catName category name will be added to the document
     * @param catValues the values string of the category
     * @return json object for the document category body
     */
    private JSONObject constructUploadDocumentBody(String catName, String catValues) {
        JSONArray categories = new JSONArray(categoryService.getCategories());
        JSONObject errorHandler = new JSONObject();
        JSONObject catInstance = null;
        for(int i = 0; i < categories.length(); i ++) {
            if(categories.getJSONObject(i).get("name").equals(catName)) {
                catInstance = categories.getJSONObject(i);
                break;
            }
        }
        if(catInstance == null) {
            errorHandler.put("error", "catName = " + catName + " does not exist");
            return errorHandler;
        }
        String [] catValuesArr = catValues.split(";");
        JSONObject attrJson = new JSONObject();
        for(String value: catValuesArr) {
            String [] attrValue = value.split("=");
            if(attrValue.length < 2) {
                errorHandler.put("error", "Invalid cat_values");
                return errorHandler;
            }
            attrValue[0] = attrValue[0].trim();
            attrValue[1] = attrValue[1].trim();
            if(!catInstance.getJSONObject("properties").has(attrValue[0])) {
                errorHandler.put("error", "cat_value = " + attrValue[0] +" does not exist");
                return errorHandler;
            }
            if(attrValue[0].contains("تاريخ")) {
                JSONObject dateConvertResult = dateConvert(attrValue[1]);
                if(dateConvertResult.has("error")) {
                    return dateConvertResult;
                }
                attrJson.put(catInstance.getJSONObject("properties").getString(attrValue[0]), dateConvertResult.get("date"));
            } else {
                attrJson.put(catInstance.getJSONObject("properties").getString(attrValue[0]), attrValue[1]);
            }

        }
        JSONObject categoryJson = new JSONObject();
        categoryJson.put(catInstance.get("id").toString(), attrJson);
        return new JSONObject().put("categories", categoryJson);
    }

    private JSONObject dateConvert(String originalDateString) {
        JSONObject result = new JSONObject();
        try {
            String replacedDateString = originalDateString.replace('/', '-');

            // Split the date string into parts
            String[] dateParts = replacedDateString.split("-");

            result.put("date", dateParts[2] + "-" + dateParts[1] + "-" + dateParts[0]);
        } catch (Exception e) {
            result.put("error", "Please enter a valid date format (dd/mm/yyyy)");
        }
        return result;
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }
}
