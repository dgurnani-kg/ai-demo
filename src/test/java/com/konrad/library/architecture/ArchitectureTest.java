package com.konrad.library.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The rules an agent cannot talk its way past.
 *
 * <p>Everything in CLAUDE.md is persuasion. This file is enforcement: it runs in `./gradlew check`
 * and a violation is a non-zero exit code regardless of how confident the explanation was.
 */
@AnalyzeClasses(packages = "com.konrad.library", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /**
     * The headline rule. A controller that reaches straight into a repository skips validation,
     * transaction boundaries and the DTO mapping that keeps us honest against the OpenAPI spec.
     */
    @ArchTest
    static final ArchRule layersAreRespected = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Web")
            .definedBy("..web..")
            .layer("Service")
            .definedBy("..service..")
            .layer("Domain")
            .definedBy("..domain..")
            .whereLayer("Web")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Service")
            .mayOnlyBeAccessedByLayers("Web")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Service")
            .as("web -> service -> domain, and never the other way round");

    /** JPA entities must not reach the wire. DTOs in ..web.dto.. are the only wire shapes. */
    @ArchTest
    static final ArchRule entitiesStayOutOfTheWebLayer = noClasses()
            .that()
            .resideInAPackage("..web..")
            .should()
            .dependOnClassesThat()
            .areAnnotatedWith("jakarta.persistence.Entity")
            .as("entities must not leak into the web layer; map to a DTO in the service or " + "controller instead")
            .because("the wire shape is owned by api/openapi.yaml, not by the database schema");

    /** Features talk in one direction only. See CopiesOnLoan for how we break a would-be cycle. */
    @ArchTest
    static final ArchRule noCyclesBetweenFeatures =
            slices().matching("com.konrad.library.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule controllersLiveInWeb = classes()
            .that()
            .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should()
            .resideInAPackage("..web..");

    @ArchTest
    static final ArchRule servicesLiveInService = classes()
            .that()
            .areAnnotatedWith("org.springframework.stereotype.Service")
            .should()
            .resideInAPackage("..service..");

    @ArchTest
    static final ArchRule repositoriesLiveInDomain =
            classes().that().haveSimpleNameEndingWith("Repository").should().resideInAPackage("..domain..");

    /** Constructor injection only, so every collaborator is visible in one place and testable. */
    @ArchTest
    static final ArchRule noFieldInjection = noFields()
            .should()
            .beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .as("inject through the constructor");

    @ArchTest
    static final ArchRule noLegacyDateApi = noClasses()
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.util.Date")
            .as("use java.time; java.util.Date is not allowed");

    @ArchTest
    static final ArchRule noPrintlnDebugging =
            noClasses().should().accessField(System.class, "out").as("use a logger, not System.out");
}
