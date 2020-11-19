package org.folio.aes.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RoutingRuleTest {
  final RoutingRule rule = new RoutingRule("$.store.book[?(@.price < 10)]", "tenant1");

  @Test
  void testRoutingRule() {
    assertAll("rule",
        () -> assertEquals("$.store.book[?(@.price < 10)]", rule.getCriteria()),
        () -> assertEquals("tenant1", rule.getTarget()),
        () -> assertEquals("RoutingRule [criteria=$.store.book[?(@.price < 10)], target=tenant1]",
            rule.toString())
    );
  }

  @Test
  void testRoutingRuleEquality() {
    final RoutingRule ruleCopy = new RoutingRule("$.store.book[?(@.price < 10)]", "tenant1");
    final RoutingRule ruleWrongCriteria = new RoutingRule("bad", "tenant1");
    final RoutingRule ruleWrongTarget = new RoutingRule(".store.book[?(@.price < 10)]", "bad");
    final Object badType = "bad";

    assertAll("equals",
        () -> assertEquals(true, rule.equals(rule)), // identity
        () -> assertEquals(true, rule.equals(ruleCopy)), // identical
        () -> assertEquals(false, rule.equals(badType)), // wrong type
        () -> assertEquals(false, rule.equals(ruleWrongCriteria)), // wrong criteria
        () -> assertEquals(false, rule.equals(ruleWrongTarget)) // wrong target
    );
  }
}
