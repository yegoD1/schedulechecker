package com.yegoD;

import java.net.http.HttpRequest;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest.Builder;

import java.nio.charset.StandardCharsets;

import java.util.Map;
import java.util.stream.Collectors;

// Call types to use.
enum HttpCallType
{
    GET,
    POST,
    PUT
}

public class HttpRequestBuilder {

    /**
     * Constructs an HttpRequest from given parameters.
     * @param URL The actual URL to call. Make sure there are no queries.
     * @param queries Array of basic pairs to be added URL as queries. Will automatically format.
     * @param headers Array of basic pairs to be sent as headers.
     * @param callType Enum for request type to use.
     * @param body Optinal argument. Creates a URL encoded form body.
     * @return
     */
    public static HttpRequest BuildCall(String URL, BasicPair<String,String>[] queries, BasicPair<String, String>[] headers, HttpCallType callType, Map<String, String> body)
    {
        Builder httpBuilder = HttpRequest.newBuilder();
        StringBuilder strBuilder = new StringBuilder(URL);

        if(queries != null && queries.length > 0)
        {
            strBuilder.append("?");
            for(BasicPair<String,String> query : queries)
            {
                strBuilder.append(query.getKey());
                strBuilder.append("=");
                strBuilder.append(query.getValue());
                strBuilder.append("&");
            }

            strBuilder.deleteCharAt(strBuilder.length()-1);
        }

        try{
            httpBuilder.uri(new URI(strBuilder.toString()));
        }
        catch(Exception e)
        {
            
        }
        

        if(headers != null)
        {
            for(BasicPair<String,String> curHeader : headers)
            {
                httpBuilder.header(curHeader.getKey(), curHeader.getValue());
            }
        }

        String callTypeString = "";

        switch (callType)
        {
            case GET:
                callTypeString = "GET";
                break;

            case POST:
                callTypeString = "POST";
                break;

            case PUT:
                callTypeString = "PUT";
                break;
        }

        if(body != null)
        {
            // Taken from https://stackoverflow.com/questions/56728398/java-11-new-http-client-send-post-requests-with-x-www-form-urlencoded-parameter
            String formBody = body.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

            // Set content-type since we are sending a body.
            httpBuilder.header("Content-Type", "application/x-www-form-urlencoded");
            httpBuilder.method(callTypeString, HttpRequest.BodyPublishers.ofString(formBody));
        }
        else
        {
            httpBuilder.method(callTypeString, HttpRequest.BodyPublishers.noBody());
        }
        
        return httpBuilder.build();
    }
}
