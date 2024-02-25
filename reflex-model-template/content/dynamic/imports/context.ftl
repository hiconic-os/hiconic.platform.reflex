<#-- We prefix helper variables (ONLY meant to be used in this file) with _, e.g. _groupPackage -->
<#function relocation classFullName>
    <#return "src/${classFullName?replace('.', '/')}.java">
</#function>

<#assign _groupPackage = request.groupId?replace('-', '')>

<#assign _aidSansModel = request.artifactId?remove_ending('-model')>
<#assign nameBaseKebab = _aidSansModel?remove_ending('-api')>
<#assign nameBaseSnake = nameBaseKebab?replace('-', '_')>
<#assign nameBasePascal = support.toPascalCase(nameBaseKebab, '-')>

<#assign basePackage = "${support.smartPackageName(_groupPackage, nameBaseSnake)}">

<#assign modelPackage = "${basePackage}.model">
<#assign modelApiPackage = "${modelPackage}.api">

<#assign greetRequestSimple = "Greet">
<#assign greetRequestFull = "${modelApiPackage}.${greetRequestSimple}">