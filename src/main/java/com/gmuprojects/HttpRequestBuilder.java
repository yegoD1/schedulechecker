package com.gmuprojects;

import java.net.http.HttpRequest;
import java.net.URI;
import java.net.http.HttpRequest.Builder;

// Call types to use.
enum HttpCallType
{
    GET,
    POST,
    PUT
}

public class HttpRequestBuilder {

    public static HttpRequest BuildCall(String URL, BasicPair<String,String>[] queries, BasicPair<String, String>[] headers, HttpCallType callType, String body)
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
            httpBuilder.method(callTypeString, HttpRequest.BodyPublishers.ofString(body));
        }
        else
        {
            httpBuilder.method(callTypeString, HttpRequest.BodyPublishers.noBody());
        }
        
        return httpBuilder.build();
    }
}
