package org.integratedmodelling.generators.library;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.integratedmodelling.klab.api.knowledge.*;
import org.integratedmodelling.klab.api.knowledge.Observable;
import org.integratedmodelling.klab.api.scope.Scope;
import org.integratedmodelling.klab.api.services.Reasoner;
import org.junit.jupiter.api.Test;

class AuthoredCategoriesTest {
  record Fixture(Observable observable, Scope scope, Reasoner reasoner, Concept family) {}
  Fixture fixture() {
    var observable = mock(Observable.class); var scope = mock(Scope.class);
    var reasoner = mock(Reasoner.class); var builder = mock(Observable.Builder.class);
    var family = mock(Concept.class);
    when(scope.getService(Reasoner.class)).thenReturn(reasoner);
    when(observable.builder(scope)).thenReturn(builder);
    when(builder.without(SemanticRole.INHERENT)).thenReturn(builder);
    when(builder.buildConcept()).thenReturn(family);
    return new Fixture(observable, scope, reasoner, family);
  }
  @Test void onlyConcreteAuthoredCandidatesAreSampledAndScopedMeaningIsPreserved() {
    var t = fixture(); var abstractValue = mock(Concept.class); var authored = mock(Concept.class);
    when(abstractValue.is(SemanticType.ABSTRACT)).thenReturn(true);
    when(authored.getUrn()).thenReturn("audit:Concrete of audit:Region");
    when(t.reasoner.authoredClosure(t.family)).thenReturn(List.of(abstractValue, authored));
    assertSame(authored, RandomContextualizers.generateConcept(t.observable, null, t.scope));
    verify(t.reasoner, never()).closure(any());
  }
  @Test void emptyAuthoredSetRetainsEmptyClassifierOutcome() {
    var t = fixture(); when(t.reasoner.authoredClosure(t.family)).thenReturn(List.of());
    assertNull(RandomContextualizers.generateConcept(t.observable, null, t.scope));
  }
  @Test void multipleCandidatesUseTheExistingRandomChoice() {
    var t = fixture(); var a = mock(Concept.class); var b = mock(Concept.class);
    when(t.reasoner.authoredClosure(t.family)).thenReturn(List.of(a, b));
    var previous = RandomContextualizers.random; var deterministic = mock(Random.class);
    when(deterministic.nextInt(2)).thenReturn(1); RandomContextualizers.random = deterministic;
    try { assertSame(b, RandomContextualizers.generateConcept(t.observable, null, t.scope)); }
    finally { RandomContextualizers.random = previous; }
  }
  @Test void unavailableAuthoredQueryCannotFallBackToGeneratedClosure() {
    var t = fixture(); when(t.reasoner.authoredClosure(t.family)).thenThrow(new UnsupportedOperationException());
    assertThrows(UnsupportedOperationException.class,
        () -> RandomContextualizers.generateConcept(t.observable, null, t.scope));
    verify(t.reasoner, never()).closure(any());
  }
}
