package com.booksearch.controllers;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.extraction.ExtractingParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

@RestController
public class SearchController {

    @Value("${solr.url}demo")
    private String url;
    private String collectionName;

    private SolrClient solr;

    public SearchController() {
        solr = new HttpSolrClient.Builder()
                .withBaseSolrUrl("http://localhost:8983/solr/demo")
                .withConnectionTimeout(10000)
                .withSocketTimeout(60000)
                .allowCompression(true)
                .build();
    }

    @RequestMapping(path = "/index/pdf", method = RequestMethod.POST, consumes = { "multipart/form-data" })
    public ResponseEntity<String> indexPdf(@RequestPart("file") MultipartFile file) {
        ResponseEntity<String> response;

        try {
            ContentStreamUpdateRequest req = new ContentStreamUpdateRequest("/update/extract");

            String fileSuffix = ".pdf";

            File tempFile = File.createTempFile("temp", fileSuffix);

            file.transferTo(tempFile);

            req.addFile(tempFile,"application/pdf");

            req.setParam(ExtractingParams.EXTRACT_ONLY, "true");
            NamedList<Object> result = solr.request(req);

            response = ResponseEntity.accepted().body(result.toString());
        } catch (Exception e) {
            response = ResponseEntity.badRequest().body("Request processing error: " + e.getMessage());
        }
        return response;
    }

    @RequestMapping(path = "/search/{query}/{page}/{size}", method = RequestMethod.GET)
    public ResponseEntity<String> searchByQuery(@RequestParam("query") String userQuery,
                                                @RequestParam("page") int pageNr,
                                                @RequestParam("size") int pageSize) {
        ResponseEntity<String> response;

        try {
            SolrQuery query = new SolrQuery().setQuery(userQuery)
                                            .addSort("lastUpdatedAt", SolrQuery.ORDER.asc)
                                            .setFacet(true)
                                            .setStart(pageNr)
                                            .setRows(pageSize);

            QueryResponse queryResponse = solr.query(query);

            response = ResponseEntity.ok(queryResponse.getResults().toString());
        } catch (Exception e) {
            response = ResponseEntity.badRequest().body("Request processing error: " + e.getMessage());
        }

        return response;
    }
}
