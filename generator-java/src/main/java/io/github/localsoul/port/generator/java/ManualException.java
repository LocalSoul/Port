package io.github.localsoul.port.generator.java;

/** Intern: {@code raw}/{@code manual} im Rumpf; das Member wird abstrakt und braucht Handarbeit. */
final class ManualException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  ManualException(String reason) {
    super(reason);
  }
}
