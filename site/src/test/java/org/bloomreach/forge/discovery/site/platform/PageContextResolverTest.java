package org.bloomreach.forge.discovery.site.platform;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageContextResolverTest {

    @Mock HstRequest request;
    @Mock HstRequestContext requestContext;
    @Mock HttpServletRequest servletRequest;
    @Mock ResolvedSiteMapItem siteMapItem;

    @BeforeEach
    void setUp() {
        lenient().when(request.getRequestContext()).thenReturn(requestContext);
        lenient().when(requestContext.getServletRequest()).thenReturn(servletRequest);
    }

    // ── pageType ─────────────────────────────────────────────────────────

    @Test
    void pageType_brxdisParamOverride_returnsOverride() {
        when(servletRequest.getParameter("brxdis_ptype")).thenReturn("pdp");

        assertEquals("pdp", PageContextResolver.pageType(request));
    }

    @Test
    void pageType_headerOverride_returnsOverride() {
        when(servletRequest.getParameter("brxdis_ptype")).thenReturn(null);
        when(request.getHeader("X-Brxdis-Ptype")).thenReturn("category");

        assertEquals("category", PageContextResolver.pageType(request));
    }

    @Test
    void pageType_paramOverrideTakesPrecedenceOverHeader() {
        when(servletRequest.getParameter("brxdis_ptype")).thenReturn("pdp");
        lenient().when(request.getHeader("X-Brxdis-Ptype")).thenReturn("category");

        assertEquals("pdp", PageContextResolver.pageType(request));
    }

    @Test
    void pageType_pidParam_returnsProduct() {
        when(servletRequest.getParameter("brxdis_ptype")).thenReturn(null);
        when(request.getHeader("X-Brxdis-Ptype")).thenReturn(null);
        when(servletRequest.getParameter("pid")).thenReturn("SKU-123");

        assertEquals("product", PageContextResolver.pageType(request));
    }

    @Test
    void pageType_qParam_returnsSearch() {
        when(servletRequest.getParameter("brxdis_ptype")).thenReturn(null);
        when(request.getHeader("X-Brxdis-Ptype")).thenReturn(null);
        when(servletRequest.getParameter("pid")).thenReturn(null);
        when(servletRequest.getParameter("q")).thenReturn("shoes");

        assertEquals("search", PageContextResolver.pageType(request));
    }

    @Test
    void pageType_rootSlashUri_returnsHomepage() {
        when(servletRequest.getParameter("brxdis_ptype")).thenReturn(null);
        when(request.getHeader("X-Brxdis-Ptype")).thenReturn(null);
        when(servletRequest.getParameter("pid")).thenReturn(null);
        when(servletRequest.getParameter("q")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/");

        assertEquals("homepage", PageContextResolver.pageType(request));
    }

    @Test
    void pageType_nullUri_returnsHomepage() {
        when(servletRequest.getParameter("brxdis_ptype")).thenReturn(null);
        when(request.getHeader("X-Brxdis-Ptype")).thenReturn(null);
        when(servletRequest.getParameter("pid")).thenReturn(null);
        when(servletRequest.getParameter("q")).thenReturn(null);
        when(request.getRequestURI()).thenReturn(null);

        assertEquals("homepage", PageContextResolver.pageType(request));
    }

    @Test
    void pageType_productUri_returnsProduct() {
        when(servletRequest.getParameter("brxdis_ptype")).thenReturn(null);
        when(request.getHeader("X-Brxdis-Ptype")).thenReturn(null);
        when(servletRequest.getParameter("pid")).thenReturn(null);
        when(servletRequest.getParameter("q")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/product/detail");

        assertEquals("product", PageContextResolver.pageType(request));
    }

    @Test
    void pageType_categoryUri_returnsCategory() {
        when(servletRequest.getParameter("brxdis_ptype")).thenReturn(null);
        when(request.getHeader("X-Brxdis-Ptype")).thenReturn(null);
        when(servletRequest.getParameter("pid")).thenReturn(null);
        when(servletRequest.getParameter("q")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/category/shoes");

        assertEquals("category", PageContextResolver.pageType(request));
    }

    @Test
    void pageType_otherUri_returnsContent() {
        when(servletRequest.getParameter("brxdis_ptype")).thenReturn(null);
        when(request.getHeader("X-Brxdis-Ptype")).thenReturn(null);
        when(servletRequest.getParameter("pid")).thenReturn(null);
        when(servletRequest.getParameter("q")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/blog/post");

        assertEquals("content", PageContextResolver.pageType(request));
    }

    // ── pageTitle ─────────────────────────────────────────────────────────

    @Test
    void pageTitle_siteMapItemWithTitle_returnsTitle() {
        when(requestContext.getResolvedSiteMapItem()).thenReturn(siteMapItem);
        when(siteMapItem.getPageTitle()).thenReturn("Search Results");

        assertEquals("Search Results", PageContextResolver.pageTitle(request, "search"));
    }

    @Test
    void pageTitle_noSiteMapItem_returnsRequestUri() {
        when(requestContext.getResolvedSiteMapItem()).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/search");

        assertEquals("/search", PageContextResolver.pageTitle(request, "search"));
    }

    @Test
    void pageTitle_blankSiteMapTitle_fallsBackToUri() {
        when(requestContext.getResolvedSiteMapItem()).thenReturn(siteMapItem);
        when(siteMapItem.getPageTitle()).thenReturn("  ");
        when(request.getRequestURI()).thenReturn("/search");

        assertEquals("/search", PageContextResolver.pageTitle(request, "search"));
    }

    @Test
    void pageTitle_rootUriWithHomepageType_returnsHome() {
        when(requestContext.getResolvedSiteMapItem()).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/");

        assertEquals("Home", PageContextResolver.pageTitle(request, "homepage"));
    }

    @Test
    void pageTitle_rootUriWithNonHomepageType_returnsPageType() {
        when(requestContext.getResolvedSiteMapItem()).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/");

        assertEquals("search", PageContextResolver.pageTitle(request, "search"));
    }

    // ── pageUrl ───────────────────────────────────────────────────────────

    @Test
    void pageUrl_port80_omitsPort() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("example.com");
        when(request.getServerPort()).thenReturn(80);
        when(request.getRequestURI()).thenReturn("/search");
        when(request.getQueryString()).thenReturn(null);

        assertEquals("http://example.com/search", PageContextResolver.pageUrl(request));
    }

    @Test
    void pageUrl_port443_omitsPort() {
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("example.com");
        when(request.getServerPort()).thenReturn(443);
        when(request.getRequestURI()).thenReturn("/search");
        when(request.getQueryString()).thenReturn(null);

        assertEquals("https://example.com/search", PageContextResolver.pageUrl(request));
    }

    @Test
    void pageUrl_nonStandardPort_includesPort() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getRequestURI()).thenReturn("/search");
        when(request.getQueryString()).thenReturn(null);

        assertEquals("http://localhost:8080/search", PageContextResolver.pageUrl(request));
    }

    @Test
    void pageUrl_withQueryString_appended() {
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("example.com");
        when(request.getServerPort()).thenReturn(443);
        when(request.getRequestURI()).thenReturn("/search");
        when(request.getQueryString()).thenReturn("q=shoes&page=2");

        assertEquals("https://example.com/search?q=shoes&page=2", PageContextResolver.pageUrl(request));
    }

    // ── originalRefUrl ────────────────────────────────────────────────────

    @Test
    void originalRefUrl_paramPresent_returnsParam() {
        when(servletRequest.getParameter("orig_ref_url")).thenReturn("https://ref.example.com");

        assertEquals("https://ref.example.com",
                PageContextResolver.originalRefUrl(request, "https://fallback.com"));
    }

    @Test
    void originalRefUrl_headerPresent_returnsHeader() {
        when(servletRequest.getParameter("orig_ref_url")).thenReturn(null);
        when(request.getHeader("X-Brxdis-Orig-Ref-Url")).thenReturn("https://header.com");

        assertEquals("https://header.com",
                PageContextResolver.originalRefUrl(request, "https://fallback.com"));
    }

    @Test
    void originalRefUrl_paramWinsOverHeader() {
        when(servletRequest.getParameter("orig_ref_url")).thenReturn("https://param.com");
        lenient().when(request.getHeader("X-Brxdis-Orig-Ref-Url")).thenReturn("https://header.com");

        assertEquals("https://param.com",
                PageContextResolver.originalRefUrl(request, "https://fallback.com"));
    }

    @Test
    void originalRefUrl_neitherPresent_returnsFallback() {
        when(servletRequest.getParameter("orig_ref_url")).thenReturn(null);
        when(request.getHeader("X-Brxdis-Orig-Ref-Url")).thenReturn(null);

        assertEquals("https://fallback.com",
                PageContextResolver.originalRefUrl(request, "https://fallback.com"));
    }

    @Test
    void originalRefUrl_nullRequestContext_returnsNull() {
        when(request.getRequestContext()).thenReturn(null);

        assertNull(PageContextResolver.originalRefUrl(request, "https://fallback.com"));
    }
}
