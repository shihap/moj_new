package com.opentext.moj.controller;


import com.opentext.moj.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/rest/myresource")
@Tag(name = "document-management")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    /**
     * upload new document to content server
     * @param file document will be uploaded
     * @param cat_name category name
     * @param cat_values category values
     * @param parent_path folder path
     * @param username content server username
     * @param password content server user password
     * @return document id
     */
    @Operation(

            description = "upload a document to content server",
            summary = "upload a document to content server",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            content = @Content(examples = {@ExampleObject(value = "{ \"id\": 123456 }")})
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            content = @Content(examples = {@ExampleObject(value = "{ \"error\": \"Invalid parameters\" }")})
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            content = @Content(examples = {@ExampleObject(value = "{ \"error\": \"Unauthorized\" }")})
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            content = @Content(examples = {@ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")})
                    )
            }
    )

    @PostMapping(value = "/CreateDocument", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createDocument(
            @Parameter(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE), description = "file will be uploaded") @RequestPart(value = "file", required = false) MultipartFile file,
            @Parameter(description = "category name") @RequestPart(value = "cat_name", required = false) String cat_name,
            @Parameter(description = "category values") @RequestPart(value = "cat_values", required = false) String cat_values,
            @Parameter(description = "folder path") @RequestPart(value = "parent_path", required = false) String parent_path,
            @Parameter(description = "content server username") @RequestPart(value = "username", required = false) String username,
            @Parameter(description = "content server username's password") @RequestPart(value = "password", required = false) String password) {
        JSONObject result = new JSONObject();
        try {
            // Trim the parameters to remove leading and trailing spaces
            if (cat_name != null) cat_name = cat_name.trim();
            if (cat_values != null) cat_values = cat_values.trim();
            if (parent_path != null) parent_path = parent_path.trim();
            if (username != null) username = username.trim();
            if (password != null) password = password.trim();

            if(file == null || file.isEmpty()) {
                result.put("error", "the file parameter is missing");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            } else if(cat_name == null || cat_name.isEmpty() || cat_values == null || cat_values.isEmpty()) {
                result.put("error", "cat_name/cat_values is invalid or missing");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            } else if(parent_path == null || parent_path.isEmpty()) {
                result.put("error", "parent_path is invalid or missing");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            } else if(username == null || password == null) {
                result.put("error", "Argument username/password is required.");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            } else if(username.isEmpty() || password.isEmpty()) {
                result.put("error", "Invalid username/password specified.");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            }
            return documentService.createDocument(file, cat_name, cat_values, parent_path, username, password);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * get document by id and version number
     * @param doc_id document id
     * @param version_no document version
     * @param username content server username
     * @param password content server user password
     * @return byte[] for the specified document
     */
    @Operation(
            description = "get a document from content server by id and version number",
            summary = "get a document from content server by id and version number",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(examples = {@ExampleObject(value = "{ \"result\": \"Base64 String\" }")})
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            content = @Content(examples = {@ExampleObject(value = "{ \"error\": \"Invalid parameters\" }")})
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            content = @Content(examples = {@ExampleObject(value = "{ \"error\": \"Unauthorized\" }")})
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            content = @Content(examples = {@ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")})
                    )
            }
    )
    @GetMapping(value = {"/RetrieveDocumentById"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getDocumentById(
            @Parameter(description = "document id") @RequestParam(value = "doc_id", required = false) String doc_id,
            @Parameter(description = "version number") @RequestParam(value = "version_no", required = false) String version_no,
            @Parameter(description = "content server username") @RequestParam(value = "username", required = false) String username,
            @Parameter(description = "content server username's password") @RequestParam(value = "password", required = false) String password) {
        JSONObject result = new JSONObject();
        try {
            // Trim the parameters to remove leading and trailing spaces
            if(doc_id != null) doc_id = doc_id.trim();
            if(version_no != null) version_no = version_no.trim();
            if (username != null) username = username.trim();
            if (password != null) password = password.trim();

            if(doc_id == null || doc_id.contains("-") || doc_id.isEmpty()) {
                result.put("error", "Missing or invalid doc_id");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            } else if (!isNumeric(doc_id)) {
                result.put("error", "doc_id accepts numbers only");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            }
            else if ((version_no != null && version_no.contains("-")) || (version_no != null && version_no.isEmpty())) {
                result.put("error", "Missing or invalid version number");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            }
            else if (version_no != null && !isNumeric(version_no)) {
                result.put("error", "Version_no accepts numbers only");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            }
            else if(username == null || password == null) {
                result.put("error", "Argument username/password is required.");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            } else if(username.isEmpty() || password.isEmpty()) {
                result.put("error", "Invalid username/password specified.");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            }
            if(version_no == null) {
                version_no = "0";
            }
            return documentService.getDocumentById(doc_id, version_no, username, password);
        } catch (HttpClientErrorException.BadRequest e) {
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * delete document by id
     * @param doc_id document id
     * @param username content server username
     * @param password content server user password
     * @return object for the action status
     */
    @Operation(
            description = "delete a document from content server by id and version number",
            summary = "delete a document from content server by id and version number",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(examples = {@ExampleObject(value = "{ \"result\": \"Document with id = 123 and version_no = 1 deleted successfully\" }")})
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            content = @Content(examples = {@ExampleObject(value = "{ \"error\": \"Invalid parameters\" }")})
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            content = @Content(examples = {@ExampleObject(value = "{ \"error\": \"Unauthorized\" }")})
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            content = @Content(examples = {@ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")})
                    )
            }
    )
    @DeleteMapping(value = {"/DeleteDocumentById"}, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> deleteDocumentById(
            @Parameter(description = "document id") @RequestPart(value = "Doc_id", required = false) String doc_id,
            @Parameter(description = "version number") @RequestPart(value = "Version_no", required = false) String version_no,
            @Parameter(description = "content server username") @RequestPart(value = "username", required = false) String username,
            @Parameter(description = "content server username's password") @RequestPart(value = "password", required = false) String password) {
        JSONObject result = new JSONObject();
        try {
            // Trim the parameters to remove leading and trailing spaces
            if(doc_id != null) doc_id = doc_id.trim();
            if(version_no != null) version_no = version_no.trim();
            if (username != null) username = username.trim();
            if (password != null) password = password.trim();

            if(doc_id == null || doc_id.isEmpty() || doc_id.contains("-")) {
                result.put("error", "Missing or invalid doc_id");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            } else if (!isNumeric(doc_id)) {
                result.put("error", "doc_id accepts numbers only");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            } else if (version_no == null || version_no.contains("-") || version_no.isEmpty()) {
                result.put("error", "Missing or invalid version number");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            } else if (!isNumeric(version_no)) {
                result.put("error", "Version_no accepts numbers only");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            } else if(username == null || password == null) {
                result.put("error", "Argument username/password is required.");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            } else if(username.isEmpty() || password.isEmpty()) {
                result.put("error", "Invalid username/password specified.");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            }
            return documentService.deleteDocumentById(doc_id, version_no, username, password);
        } catch (HttpClientErrorException.BadRequest e) {
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * create new version of a document
     * @param doc_id document id
     * @param file the document that will be the new version
     * @param username content server username
     * @param password content server user password
     * @return the document id and version number
     */
    @Operation(
            description = "import a new document version",
            summary = "import a new document version",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(examples = {@ExampleObject(value = "{ \"result\": { \"version_number\": 2, \"id\": 12345 } }")})
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            content = @Content(examples = {@ExampleObject(value = "{ \"error\": \"Invalid parameters\" }")})
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            content = @Content(examples = {@ExampleObject(value = "{ \"error\": \"Unauthorized\" }")})
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            content = @Content(examples = {@ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")})
                    )
            }
    )
    @PostMapping(value = {"/ImportNewVersionByID"}, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importVersionById(
            @Parameter(description = "document id") @RequestPart(value = "doc_id", required = false) String doc_id,
            @Parameter(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE), description = "file will be uploaded") @RequestPart(value = "file", required = false) MultipartFile file,
            @Parameter(description = "content server username") @RequestPart(value = "username", required = false) String username,
            @Parameter(description = "content server username's password") @RequestPart(value = "password", required = false) String password) throws HttpClientErrorException {
        JSONObject result = new JSONObject();
        try {
            // Trim the parameters to remove leading and trailing spaces
            if (doc_id != null) doc_id = doc_id.trim();
            if (username != null) username = username.trim();
            if (password != null) password = password.trim();

            if(doc_id == null || doc_id.isEmpty() || doc_id.contains("-")) {
                result.put("error", "Missing or invalid doc_id");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            } else if (!isNumeric(doc_id)) {
                result.put("error", "doc_id accepts numbers only");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            } else if(file == null || file.isEmpty()) {
                result.put("error", "the file parameter is missing");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            } else if(username == null || password == null) {
                result.put("error", "Argument username/password is required.");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            } else if(username.isEmpty() || password.isEmpty()) {
                result.put("error", "Invalid username/password specified.");
                return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
            }
            return documentService.importVersionById(doc_id, file, username, password);
        } catch (HttpClientErrorException.BadRequest e) {
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result.toString(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
