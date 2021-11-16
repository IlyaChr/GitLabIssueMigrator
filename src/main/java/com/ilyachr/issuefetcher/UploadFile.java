package com.ilyachr.issuefetcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;

public class UploadFile {

    /**
     * @param projectPath
     * @param projectId
     * @param token
     * @return new upload file path
     * @throws IOException
     */
    public String uploadFile(String fileName,String docPath, String projectPath, String projectId, String token) throws IOException {
        HttpUtils multipart = new HttpUtils(projectPath, projectId, token);
        multipart.addFilePart(fileName,"file", new File(docPath));
        String response = multipart.finish();
        return getNewFilePath(response);
    }


    public String getNewFilePath(String response) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.readValue(response, ObjectNode.class);
        return node.has("url") ? node.get("url").textValue() : "";
    }

}
