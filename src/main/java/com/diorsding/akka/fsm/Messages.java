package com.diorsding.akka.fsm;


import akka.actor.ActorRef;

public class Messages {

    /**
     * Chopstick messages
     */
    public static final class Busy {
        public final ActorRef chopstick;
        public Busy(ActorRef chopstick){
            this.chopstick = chopstick;
        }
    }

    public static final class Taken {
        public final ActorRef chopstick;
        public Taken(ActorRef chopstick){
            this.chopstick = chopstick;
        }
    }

    /**
     * Philosopher messages  -> object instance of concrete class. Use class instead
     */
    private static interface PutMessage {};
    public static final Object Put = new PutMessage() {
        @Override
        public String toString() { return "Put"; }
    };

    private static interface TakeMessage {};
    public static final Object Take = new TakeMessage() {
        @Override
        public String toString() { return "Take"; }
    };

    private static interface ThinkMessage {};
    public static final Object Think = new ThinkMessage() {
        @Override
        public String toString() { return "Think"; }
    };
}
