<?xml version="1.0" encoding="UTF-8" ?>
<!--
  - Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -
  -->
<p:pipeline version="1.0"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:calli ="http://callimachusproject.org/rdf/2009/framework#">

    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />

    <p:option name="target" required="true"  />
    <p:option name="query" select="''" />

    <p:import href="page-layout-html.xpl" />

    <p:xslt>
        <p:with-param name="target"   select="$target" />
        <p:with-param name="systemId" select="$target" />
        <p:with-param name="query" select="$query" />
        <p:input port="stylesheet">
            <p:document href="../transforms/describe.xsl" />
        </p:input>
    </p:xslt>

    <calli:page-layout-html>
        <p:with-option name="target" select="$target" />
        <p:with-option name="query" select="$query" />
    </calli:page-layout-html>
</p:pipeline>
