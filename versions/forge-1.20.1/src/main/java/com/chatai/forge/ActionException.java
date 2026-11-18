package com.chatai.forge;

/** A failure whose message is safe to show to the player. */
public final class ActionException extends Exception {
    public ActionException(String msg) { super(msg); }
}
