package br.org.cria.splinkerapp.services.interfaces;

import br.org.cria.splinkerapp.models.DataSource;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import javafx.concurrent.Service;
public interface IDarwinCoreArchiveService {
    static List<String> getDarwinCoreFields() {
        return Arrays.asList("id", "type", "modified", "language", "license", "rightsHolder",
                "accessRights", "bibliographicCitation", "references", "institutionID",
                "collectionID", "datasetID", "institutionCode", "collectionCode",
                "datasetName", "ownerInstitutionCode", "basisOfRecord", "informationWithheld",
                "dataGeneralizations", "dynamicProperties", "occurrenceID", "catalogNumber",
                "recordNumber", "recordedBy", "individualCount", "sex", "lifeStage",
                "reproductiveCondition", "behavior", "establishmentMeans", "occurrenceStatus",
                "preparations", "disposition", "associatedMedia", "associatedReferences",
                "associatedSequences", "associatedTaxa", "otherCatalogNumbers",
                "occurrenceRemarks", "organismID", "previousIdentifications", "organismName",
                "organismScope", "associatedOccurrences", "associatedOrganisms",
                "hostOrganismID", "recordedByIDs", "samplingProtocol", "samplingEffort",
                "sampleSizeValue", "sampleSizeUnit", "fieldNumber", "fieldNotes",
                "eventDate", "eventTime", "startDayOfYear", "endDayOfYear", "year",
                "month", "day", "verbatimEventDate", "habitat", "samplingStrategy",
                "measurementDeterminedBy", "measurementMethod", "measurementRemarks",
                "measurementType", "measurementUnit", "measurementValue",
                "dissolvedOxygen", "depth", "temperature", "salinity", "ph", "locationID",
                "higherGeographyID", "higherGeography", "continent", "waterBody", "islandGroup",
                "island", "country", "countryCode", "stateProvince", "county", "municipality",
                "locality", "verbatimLocality", "minimumDepthInMeters", "maximumDepthInMeters",
                "verbatimDepth", "decimalLatitude", "decimalLongitude", "geodeticDatum",
                "coordinateUncertaintyInMeters", "coordinatePrecision", "pointRadiusSpatialFit",
                "footprintWKT", "footprintSRS", "footprintSpatialFit", "georeferencedBy", "georeferenceProtocol",
                "georeferenceSources", "georeferenceVerificationStatus", "georeferenceRemarks", "identifiedBy",
                "dateIdentified", "identificationReferences", "identificationRemarks", "taxonID", "scientificNameID",
                "acceptedNameUsageID", "parentNameUsageID", "originalNameUsageID", "nameAccordingToID", "namePublishedInID",
                "taxonConceptID", "scientificName", "acceptedNameUsage", "parentNameUsage", "originalNameUsage", "nameAccordingTo",
                "namePublishedIn", "namePublishedInYear", "higherClassification", "kingdom", "phylum", "classs", "order", "family",
                "genus", "subgenus", "specificEpithet", "infraspecificEpithet", "taxonRank", "verbatimTaxonRank", "vernacularName",
                "nomenclaturalCode", "taxonomicStatus", "nomenclaturalStatus", "taxonRemarks");
    }

    IDarwinCoreArchiveService generateTXTFile();
    IDarwinCoreArchiveService generateZIPFile();
    IDarwinCoreArchiveService readDataFromSource(DataSource source);
    Service transferData() throws FileNotFoundException;
}
