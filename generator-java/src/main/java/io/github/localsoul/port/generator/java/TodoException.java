package io.github.localsoul.port.generator.java;

/**
 * Intern: ein Konstrukt lässt sich (noch) nicht in Java erzeugen. Wird auf Member- bzw. Typebene
 * gefangen und zu einem {@link Todo}.
 */
final class TodoException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  TodoException(String reason) {
    super(reason);
  }
}
