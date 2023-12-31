package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class HotelSearchTest {
    private RestHighLevelClient client;

    @Test
    void testMatchAll() throws IOException {
        //1.准备request
        SearchRequest request = new SearchRequest("hotel");
        //2.准备DSL
        request.source().query(QueryBuilders.matchAllQuery());
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.解析结果
        handleResponse(response);
    }


    @Test
    void testMatch() throws IOException {
        //1.准备request
        SearchRequest request = new SearchRequest("hotel");
        //2.准备DSL
        request.source().query(QueryBuilders.matchQuery("all", "如家"));
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }


    @Test
    void testBool() throws IOException {
        //1.准备request
        SearchRequest request = new SearchRequest("hotel");
        //2.准备DSL
        //2.1准备booleanQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //2.2准备term
        boolQuery.must(QueryBuilders.termQuery("city", "杭州"));
        //2.3准备range
        boolQuery.filter(QueryBuilders.rangeQuery("price").lte(250));
        request.source().query(boolQuery);
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }


    @Test
    void testPageAndSort() throws IOException {
        //页码，每页大小
        int page = 2, size = 5;
        //1.准备request
        SearchRequest request = new SearchRequest("hotel");
        //2.准备DSL
        //2.1准备query
        request.source().query(QueryBuilders.matchAllQuery());
        //2.2准备sort
        request.source().sort("price", SortOrder.ASC);
        //2.3分页form、size
        request.source().from((page - 1) * size).size(5);
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }


    @Test
    void testHighLight() throws IOException {
        //1.准备request
        SearchRequest request = new SearchRequest("hotel");
        //2.准备DSL
        //2.1准备query
        request.source().query(QueryBuilders.matchQuery("all", "如家"));
        //2.2高亮
        request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false));
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.解析响应
        handleResponse(response);
    }


    private void handleResponse(SearchResponse response) {
        //4.解析结果
        SearchHits searchHits = response.getHits();
        //4.1获取总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("共搜索到" + total + "条数据。");
        //4.2文档数量
        SearchHit[] hits = searchHits.getHits();
        //4.3遍历
        for (SearchHit hit : hits) {
            //取source
            String json = hit.getSourceAsString();
            //json转成对象
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            //获取高亮结果
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (!CollectionUtils.isEmpty(highlightFields)) {
                HighlightField field = highlightFields.get("name");
                if (field != null) {
                    String name = field.getFragments()[0].string();
                    hotelDoc.setName(name);
                }
            }
            System.out.println("hotelDoc: " + hotelDoc);
        }
    }

    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.150.129:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }
}
