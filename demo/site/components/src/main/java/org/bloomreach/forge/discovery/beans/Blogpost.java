package org.bloomreach.forge.discovery.beans;
/*
 * Copyright 2014-2026 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.util.Calendar;
import java.util.List;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.onehippo.cms7.essentials.components.model.Authors;
import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;

@HippoEssentialsGenerated(internalName = "demo:blogpost")
@Node(jcrType = "demo:blogpost")
public class Blogpost extends HippoDocument implements Authors {

    public static final String TITLE = "demo:title";
    public static final String INTRODUCTION = "demo:introduction";
    public static final String CONTENT = "demo:content";
    public static final String PUBLICATION_DATE = "demo:publicationdate";
    public static final String CATEGORIES = "demo:categories";
    public static final String AUTHOR = "demo:author";
    public static final String AUTHOR_NAMES = "demo:authornames";
    public static final String LINK = "demo:link";
    public static final String AUTHORS = "demo:authors";
    public static final String TAGS = "hippostd:tags";

   @HippoEssentialsGenerated(internalName = "demo:publicationdate")
    public Calendar getPublicationDate() {
        return getSingleProperty(PUBLICATION_DATE);
    }

    @HippoEssentialsGenerated(internalName = "demo:authornames")
    public String[] getAuthorNames() {
        return getMultipleProperty(AUTHOR_NAMES);
    }

    public String getAuthor() {
        final String[] authorNames = getAuthorNames();
        if(authorNames !=null && authorNames.length > 0){
            return authorNames[0];
        }
        return null;
    }

    @HippoEssentialsGenerated(internalName = "demo:title")
    public String getTitle() {
        return getSingleProperty(TITLE);
    }

    @HippoEssentialsGenerated(internalName = "demo:content")
    public HippoHtml getContent() {
        return getHippoHtml(CONTENT);
    }

    @HippoEssentialsGenerated(internalName = "demo:introduction")
    public String getIntroduction() {
        return getSingleProperty(INTRODUCTION);
    }

    @HippoEssentialsGenerated(internalName = "demo:categories")
    public String[] getCategories() {
        return getMultipleProperty(CATEGORIES);
    }

    @Override
    @HippoEssentialsGenerated(internalName = "demo:authors")
    public List<Author> getAuthors() {
        return getLinkedBeans(AUTHORS, Author.class);
    }
}