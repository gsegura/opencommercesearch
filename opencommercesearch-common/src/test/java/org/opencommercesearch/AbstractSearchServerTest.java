package org.opencommercesearch;

import atg.multisite.Site;
import atg.repository.RepositoryItem;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.GroupCommand;
import org.apache.solr.client.solrj.response.GroupResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opencommercesearch.junit.SearchTest;
import org.opencommercesearch.junit.runners.SearchJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author rmerizalde
 */
@Category(IntegrationSearchTest.class)
@RunWith(SearchJUnit4ClassRunner.class)
public class AbstractSearchServerTest {

    @Mock
    private Site site;

    @Mock
    private RepositoryItem catalog;

    @Before
    public void setUp() {
        initMocks(this);
        when(site.getRepositoryId()).thenReturn("outdoorSite");
        when(site.getPropertyValue("defaultCatalog")).thenReturn(catalog);
        when(catalog.getRepositoryId()).thenReturn("outdoor");
    }

    @SearchTest(newInstance = true, productData = "/product_catalog/sandal.xml")
    public void testSearchCategoryName(SearchServer server) throws SearchServerException {
        testSearchCategoryAux(server, "shoe", "TNF3137");
    }

    @SearchTest(newInstance = true, productData = "/product_catalog/sandal.xml")
    public void testSearchCategoryNameSynonyms(SearchServer server) throws SearchServerException {
        testSearchCategoryAux(server, "sneaker", "TNF3137");
    }

    private void testSearchCategoryAux(SearchServer server, String term, String expectedProductId) throws SearchServerException {
        SolrQuery query = new SolrQuery(term);
        SearchResponse res = server.search(query, site);
        QueryResponse queryResponse = res.getQueryResponse();
        GroupResponse groupResponse = queryResponse.getGroupResponse();

        for (GroupCommand command : groupResponse.getValues()) {
            for (Group group : command.getValues()) {
                String productId = group.getGroupValue();
                if (expectedProductId.equals(productId)) {
                    return;
                }
            }
        }
        fail("Product TNF3137 not found");
    }

}
