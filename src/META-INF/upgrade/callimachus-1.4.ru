PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl:<http://www.w3.org/2002/07/owl#>
PREFIX skos:<http://www.w3.org/2004/02/skos/core#>
PREFIX sd:<http://www.w3.org/ns/sparql-service-description#>
PREFIX void:<http://rdfs.org/ns/void#>
PREFIX foaf:<http://xmlns.com/foaf/0.1/>
PREFIX msg:<http://www.openrdf.org/rdf/2011/messaging#>
PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>
PREFIX prov:<http://www.w3.org/ns/prov#>
PREFIX audit:<http://www.openrdf.org/rdf/2012/auditing#>

INSERT {
<../> calli:hasComponent <../markdown-editor.html> .
<../markdown-editor.html> a <types/Purl>, calli:Purl ;
	rdfs:label "markdown-editor.html";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<pages/text-editor.html#markdown>) AS ?alternate)
	FILTER NOT EXISTS { <../markdown-editor.html> a calli:Purl }
};

DELETE {
    ?folder a calli:PathSegment
} INSERT {
    ?folder a calli:Folder
} WHERE {
    ?folder a calli:PathSegment
};

DELETE {
    ?folder a <../1.5/types/PathSegment>
} INSERT {
    ?folder a <../1.5/types/Folder>
} WHERE {
    ?folder a <../1.5/types/PathSegment>
};

DELETE {
?dataset void:uriLookupEndpoint </sparql?uri=>.
} INSERT {
?void calli:subscriber </auth/groups/power>, </auth/groups/admin>.
?dataset void:uriLookupEndpoint </sparql?resource=>.
} WHERE {
    BIND (iri(concat(str($origin),".well-known/void")) AS ?void)
    BIND (iri(concat(str($origin),".well-known/void#dataset")) AS ?dataset)
    ?void foaf:primaryTopic ?dataset.
    ?dataset a void:Dataset;
        void:sparqlEndpoint </sparql>;
        void:uriLookupEndpoint </sparql?uri=>.
};

DELETE {
<../forbidden.html> calli:alternate ?previously
} INSERT {
<../forbidden.html> calli:alternate ?currently
} WHERE {
    BIND (str(<pages/forbidden.html>) AS ?currently)
    <../forbidden.html> calli:alternate ?previously
    FILTER (str(<pages/forbidden.xhtml?html>) = ?previously)
};

INSERT {
    </auth/invited-users/> calli:contributor </auth/groups/power>, </auth/groups/staff>
} WHERE {
    </auth/invited-users/> a calli:Folder
	FILTER NOT EXISTS {
	    </auth/invited-users/> calli:contributor </auth/groups/power>, </auth/groups/staff>
	}
};

# Update /callimachus/profile
DELETE {
    <../profile> a <../1.4/types/RdfProfile>
} INSERT {
    <../profile> a <../1.5/types/RdfProfile>
} WHERE {
    <../profile> a <../1.4/types/RdfProfile>
};

# Update Folders (/callimachus/ is excluded from auto-upgrade)
DELETE {
    <../> a <../1.4/types/Folder>
} INSERT {
    <../> a <../1.5/types/Folder>
} WHERE {
    <../> a <../1.4/types/Folder>
};

# Update PURL alternate targets
DELETE {
    ?purl a <../1.4/types/Purl>; calli:alternate ?previous
} INSERT {
    ?purl a <../1.5/types/Purl>; calli:alternate ?currently
} WHERE {
    { <../> calli:hasComponent ?purl } UNION { </> calli:hasComponent ?purl }
    ?purl a <../1.4/types/Purl>; calli:alternate ?previous .
    FILTER strstarts(str(?previous), str(</callimachus/1.4/>))
    BIND (concat(str(<../1.5/>), strafter(str(?previous),str(<../1.4/>))) AS ?currently)
};

DELETE {
    <../error.xpl> a calli:Purl,  <../1.4/types/Purl>
} INSERT {
    <../error.xpl> a calli:Proxy,  <../1.5/types/Proxy>;
        calli:post ?alternate
} WHERE {
    <../error.xpl> a calli:Purl; calli:alternate ?alternate
	FILTER NOT EXISTS { <../error.xpl> a calli:Proxy }
};

# Setup process determins the Callimachus webapp location based on Origin path
DELETE {
	</> a <../1.4/types/Origin>
} INSERT {
	</> a <../1.5/types/Origin>
} WHERE {
	</> a <../1.4/types/Origin>
};

# Setup process determins upgrade file based on versionInfo
DELETE {
	</callimachus/ontology> owl:versionInfo "1.4"
} INSERT {
	</callimachus/ontology> owl:versionInfo "1.5"
} WHERE {
	</callimachus/ontology> owl:versionInfo "1.4"
};

