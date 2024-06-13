# moj
API for Integration with OpenText Content Server

## Environment variables
```
BASE_URL=http://{CS_SERVER_IP}:{CS_PORT}/OTCS/cs.exe/api/
WEB_REPORT_ID={web report for getting folder id from path}

** SQL Server Database Settings **
spring.datasource.url=jdbc:sqlserver://{CS_SERVER_IP}:1433;databaseName={CS DB Name};encrypt=true;trustServerCertificate=true;
spring.datasource.username={CS_USER}
spring.datasource.password={CS_PASSWORD}
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl

** Oracle Database Settings **
spring.datasource.url=jdbc:oracle:thin:@{database}:1521:orcl
spring.datasource.username={db user}
spring.datasource.password={db password}
spring.datasource.driver.class-name=oracle.jdbc.OracleDriver
```
## API Endpoints

#### Base URL : ``http://{hostname or ip}:{Port number}/MOJ/rest/resource``

#### 1. Create document on Content Server :
*  `POST /CreateDocument`
* **Request:**
    - **Method:** `POST`
    - **Endpoint:** `/CreateDocument`
    - **Content Type:** `multipart/form-data`

* **Request Parameters:**
    - `file` (type: File) - The document to be uploaded to content server.
    - `username` (type: String) - Content server username.
    - `password` (type: String) - Content server password.
    - `cat_name` (type: String) - Category name will be added to the document.
    - `cat_values` (type: String) - The Category values (ex: `name=john;phone=0123;id=297`).
    - `parent_path` (type: String) - The Path folder for the document will be added in (ex: `Enterprise/ISFP/Smart CMS/أرشيف الدعاوى`).
* **Example Response**
```json
{
   "id": 123456
}
```

#### 2. retrieve document from Content Server :
*  `GET /RetrieveDocumentById`
* **Request:**
    - **Method:** `GET`
    - **Endpoint:** `/RetrieveDocumentById`
    - **Content Type:** `form-data`

* **Request Parameters:**
    - `username` (type: String) - Content server username.
    - `password` (type: String) - Content server password.
    - `doc_id` (type: String) - ID of the document will be retrieved.
    - `version_no` (type: String) - Document version number.

* **Example Response**
```json
{
  "Document_Name": "install.pdf",
  "Document_Content": "QTwxLD8sJ3Byb3ZpZGVySW5mbyc9J2l4b3M6Ly9BcmNoaXZlQ2VudGVyQGFhYWFlbnV1azNmZ2d4NHV6cWFhYWlxa2FhZnVhJywnc3RvcmFnZVByb3ZpZGVyTmFtZSc9J0VudGVycHJpc2VBcmNoaXZlJywnc3ViUHJvdmlkZXJOYW1lJz0nQXJjaGl2ZVN0b3JhZ2UnPg=="
}
```

#### 3. retrieve document from Content Server :
*  `POST /ImportNewVersionByID`
* **Request:**
    - **Method:** `POST`
    - **Endpoint:** `/ImportNewVersionByID`
    - **Content Type:** `multipart/form-data`

* **Request Parameters:**
    - `file` (type: File) - The new version of document to be uploaded to content server.
    - `username` (type: String) - Content server username.
    - `password` (type: String) - Content server password.
    - `doc_id` (type: String) - ID of the document will add version to it.

* **Example Response**
```json
{
  "result": {
    "version_number": 2,
    "id": 354795
  }
}
```

#### 4. delete document from Content Server :
*  `DELETE /DeleteDocumentById`
* **Request:**
    - **Method:** `DELETE`
    - **Endpoint:** `/DeleteDocumentById`
    - **Content Type:** `form-data`

* **Request Parameters:**
    - `username` (type: String) - Content server username.
    - `password` (type: String) - Content server password.
    - `doc_id` (type: String) - ID of the document will be deleted.
    - `version_no` (type: String) - Document version number will be deleted.

* **Example Response**
```json
{
  "result": "Document with id = 354795 and version_no = 2 deleted successfully"
}
```
## Swagger Documentation
```
swagger documentation : http://localhost:8080/swagger-ui.html
```