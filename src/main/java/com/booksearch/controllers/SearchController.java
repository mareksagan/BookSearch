package com.booksearch.controllers;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.extraction.ExtractingParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
public class SearchController {
    private SolrClient solr;

    public SearchController() {
        solr = new HttpSolrClient.Builder()
                .withBaseSolrUrl("http://localhost:8983/solr/books")
                .withConnectionTimeout(10000)
                .withSocketTimeout(60000)
                .allowCompression(true)
                .build();
    }

    @RequestMapping(path = "/index/pdf", method = RequestMethod.POST, consumes = { "multipart/form-data" })
    public ResponseEntity<String> indexPdf(@RequestPart("file") MultipartFile file) {
        ResponseEntity<String> response;

        try {
            String fileSuffix = ".pdf";

            File tempFile = File.createTempFile("temp", fileSuffix);

            file.transferTo(tempFile);

            UpdateResponse res = null;
            PdfReader reader = new PdfReader(tempFile);
            PdfDocument pdfDocument = new PdfDocument(reader);

            int pageCount = pdfDocument.getNumberOfPages();

            for (int i = 1; i <= pageCount; i++) {
                SolrInputDocument doc = new SolrInputDocument();
                doc.addField("page", i);

                // Extracting the content from a particular page
                String pageText = PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i));
                doc.addField("text", pageText);

                doc.addField("title", pdfDocument.getDocumentInfo().getTitle());
                doc.addField("author", pdfDocument.getDocumentInfo().getAuthor());

                solr.add(doc);

                if (i % 100 == 0) res = solr.commit();  // periodically flush
            }

            res = solr.commit();

            reader.close();

            response = ResponseEntity.accepted().body(res.toString());
        } catch (Exception e) {
            response = ResponseEntity.badRequest().body("Request processing error: " + e.getMessage());
        }

        return response;
    }

    @RequestMapping(path = "/search/{query}", method = RequestMethod.GET)
    public ResponseEntity<String> searchByQuery(@PathVariable("query") String userQuery/*,
                                                @RequestParam("page") int pageNr,
                                                @RequestParam("size") int pageSize*/) {
        ResponseEntity<String> response;

        try {
            SolrQuery query = new SolrQuery().setQuery(userQuery)
                                            //.addSort("author", SolrQuery.ORDER.asc)
                                            .setFacet(true);
                                            //.setStart(pageNr)
                                            //.setRows(pageSize);

            QueryResponse queryResponse = solr.query(query);

            if (queryResponse.getResults().size() == 0) throw new Exception("No results found");

            StringBuilder solrResults = new StringBuilder();

            for (SolrDocument elem : queryResponse.getResults()) {
                solrResults.append(elem.getFieldValue("book_content"));
            }

            response = ResponseEntity.ok(solrResults.toString());
        } catch (Exception e) {
            response = ResponseEntity.badRequest().body("Request processing error: " + e.getMessage());
        }

        return response;
    }
}
