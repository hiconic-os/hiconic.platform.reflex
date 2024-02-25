<#-- We prefix helper variables (ONLY meant to be used in this file) with _, e.g. _groupPackage -->
<#function relocation classFullName>
    <#return "src/${classFullName?replace('.', '/')}.java">
</#function>

<#assign groupId = request.groupId>
<#assign _groupPackage = groupId?replace("-", "")>
<#assign _artifactIdPascal = support.toPascalCase(request.artifactId, '-')>

<#assign nameBaseKebab = request.artifactId?remove_ending("-rx-module")>
<#assign nameBaseSnake = nameBaseKebab?replace("-", "_")>
<#assign nameBasePascal = support.toPascalCase(nameBaseKebab, '-')>
<#assign nameBaseCamel = nameBasePascal?uncap_first>

<#assign basePackage = "${support.smartPackageName(_groupPackage, nameBaseSnake)}">

<#assign wirePackage = "${basePackage}.wire">
<#assign spacePackage = "${wirePackage}.space">
<#assign modelPackage = "${basePackage}.model">
<#assign modelApiPackage = "${modelPackage}.api">
<#assign processingPackage = "${basePackage}.processing">

<#assign wireModuleSimple = "${_artifactIdPascal}">
<#assign wireModuleFull = "${wirePackage}.${wireModuleSimple}">

<#assign spaceSimple = "${_artifactIdPascal}Space">
<#assign spaceFull = "${spacePackage}.${spaceSimple}">

<#assign greetRequestSimple = "Greet">
<#assign greetRequestFull = "${modelApiPackage}.${greetRequestSimple}">

<#assign greetProcessorSimple = "GreetProcessor">
<#assign greetProcessorFull = "${processingPackage}.${greetProcessorSimple}">

<#assign modelReflectionSimple = "_${context.nameBasePascal}Model_">
<#assign modelReflectionFull = "${context.groupId}.${modelReflectionSimple}">