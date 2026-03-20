package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryCategoryBean;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryCategoryHighlightComponentInfo;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryCategoryHighlightComponentTest {

    @Mock HstRequest request;
    @Mock HstResponse response;

    @Test
    void noPaths_setsEmptyList() {
        new TestableCategoryHighlightComponent(new DiscoveryCategoryBean[0]).doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DiscoveryCategoryBean>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(request).setModel(eq("categories"), captor.capture());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    void onePath_resolvesOneBean() {
        DiscoveryCategoryBean bean = mock(DiscoveryCategoryBean.class);
        new TestableCategoryHighlightComponent(bean).doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DiscoveryCategoryBean>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(request).setModel(eq("categories"), captor.capture());
        assertEquals(1, captor.getValue().size());
        assertSame(bean, captor.getValue().get(0));
    }

    @Test
    void multiplePaths_collectsAllBeans() {
        DiscoveryCategoryBean b1 = mock(DiscoveryCategoryBean.class);
        DiscoveryCategoryBean b2 = mock(DiscoveryCategoryBean.class);
        new TestableCategoryHighlightComponent(b1, b2).doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DiscoveryCategoryBean>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(request).setModel(eq("categories"), captor.capture());
        assertEquals(2, captor.getValue().size());
    }

    @Test
    void nullBean_skipped() {
        DiscoveryCategoryBean b1 = mock(DiscoveryCategoryBean.class);
        TestableCategoryHighlightComponent comp = new TestableCategoryHighlightComponent(b1, null);
        comp.doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DiscoveryCategoryBean>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(request).setModel(eq("categories"), captor.capture());
        assertEquals(1, captor.getValue().size());
    }

    // ── testable subclass ──────────────────────────────────────────────────────

    private static class TestableCategoryHighlightComponent extends DiscoveryCategoryHighlightComponent {

        private final DiscoveryCategoryBean[] beans;
        private int callCount = 0;

        TestableCategoryHighlightComponent(DiscoveryCategoryBean... beans) {
            this.beans = beans;
        }

        @Override
        protected DiscoveryCategoryHighlightComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoveryCategoryHighlightComponentInfo() {
                @Override public String getDocument1() { return beans.length > 0 ? "doc1" : ""; }
                @Override public String getDocument2() { return beans.length > 1 ? "doc2" : ""; }
                @Override public String getDocument3() { return beans.length > 2 ? "doc3" : ""; }
                @Override public String getDocument4() { return beans.length > 3 ? "doc4" : ""; }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T extends HippoBean> T getHippoBeanForPath(HstRequest request, String path, Class<T> beanClass) {
            if (callCount < beans.length) {
                return beanClass.cast(beans[callCount++]);
            }
            return null;
        }
    }
}
