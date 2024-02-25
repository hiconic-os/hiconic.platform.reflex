<#-- We prefix helper variables (ONLY meant to be used in this file) with _, e.g. _groupPackage -->
<#function relocation classFullName>
    <#return "src/${classFullName?replace('.', '/')}.java">
</#function>

<#assign _groupPackage = request.groupId?replace("-", "")>
<#assign _artifactIdPascal = support.toPascalCase(request.artifactId, '-')>

<#assign artifactIdSpaced = request.artifactId?replace("-", " ")>>

<#assign nameBaseKebab = request.artifactId?remove_ending("-rx-app")?remove_ending("-app")>
<#assign nameBaseSnake = nameBaseKebab?replace("-", "_")>
<#assign nameBasePascal = support.toPascalCase(nameBaseKebab, '-')>
<#assign nameBaseCamel = nameBasePascal?uncap_first>

<#assign basePackage = "${support.smartPackageName(_groupPackage, nameBaseSnake)}">

<#assign launchFileName = "${request.artifactId}.launch">
<#assign projectName = "${request.artifactId} - ${request.groupId}">