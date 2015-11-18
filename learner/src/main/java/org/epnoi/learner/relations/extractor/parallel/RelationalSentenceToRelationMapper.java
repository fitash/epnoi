package org.epnoi.learner.relations.extractor.parallel;

import gate.Document;
import org.epnoi.learner.OntologyLearningParameters;
import org.epnoi.learner.relations.patterns.RelationalPattern;
import org.epnoi.learner.relations.patterns.lexical.BigramSoftPatternModel;
import org.epnoi.learner.relations.patterns.lexical.LexicalRelationalPatternGenerator;
import org.epnoi.learner.terms.TermCandidateBuilder;
import org.epnoi.model.*;
import org.epnoi.uia.commons.GateUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RelationalSentenceToRelationMapper {
    private BigramSoftPatternModel softPatternModel;
    private double THRESHOLD;
    private String domain;


    //------------------------------------------------------------------------------------------------------------------


    public RelationalSentenceToRelationMapper(OntologyLearningParameters parameters) {
        this.softPatternModel = (BigramSoftPatternModel) parameters.getParameterValue(OntologyLearningParameters.HYPERNYM_MODEL);
        this.THRESHOLD = (double) parameters.getParameterValue(OntologyLearningParameters.HYPERNYM_RELATION_EXTRACTION_THRESHOLD);
        this.domain = (String) parameters.getParameterValue(OntologyLearningParameters.TARGET_DOMAIN);
    }
    //------------------------------------------------------------------------------------------------------------------


    public Iterable<Relation> call(RelationalSentence relationalSentence) throws Exception {
        List<Relation> foundRelations = new ArrayList<>();
        LexicalRelationalPatternGenerator patternsGenerator = new LexicalRelationalPatternGenerator();

        List<RelationalPattern> generatedPatterns = patternsGenerator
                .generate(relationalSentence);

        Iterator<RelationalPattern> generatedPatternsIt = generatedPatterns.iterator();
        double relationhood = 0;
        while (generatedPatternsIt.hasNext()) {
            relationhood = Math.max(relationhood, this.softPatternModel
                    .calculatePatternProbability(generatedPatternsIt.next()));
        }
        if (relationhood > THRESHOLD) {
            Relation relation = _createRelation(relationalSentence, relationhood);
            foundRelations.add(relation);
        }

        return foundRelations;
    }

    //------------------------------------------------------------------------------------------------------------------

    private Relation _createRelation(RelationalSentence relationalSentence,
                                     double relationhood) {

        Document relationalSentenceDocument = GateUtils.deserializeGATEDocument(relationalSentence.getAnnotatedSentence());
        TermCandidateBuilder termCandidateBuilder = new TermCandidateBuilder(relationalSentenceDocument);

        Relation relation = new Relation();
        AnnotatedWord<TermMetadata> sourceTerm = termCandidateBuilder.buildTermCandidate(relationalSentence.getSource());
        AnnotatedWord<TermMetadata> targetTerm = termCandidateBuilder.buildTermCandidate(relationalSentence.getTarget());

        String relationURI = Relation.buildURI(sourceTerm
                .getWord(), targetTerm.getWord(), RelationHelper.HYPERNYM, domain);


        relation.setUri(relationURI);
        relation.setSource(Term.buildURI(sourceTerm.getWord(), domain));
        relation.setTarget(Term.buildURI(targetTerm.getWord(), domain));
        relation.setType(RelationHelper.HYPERNYM);
        relation.addProvenanceSentence(relationalSentence.getSentence(), relationhood);

        return relation;
    }

    //------------------------------------------------------------------------------------------------------------------

}
