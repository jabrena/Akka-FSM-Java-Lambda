package com.diorsding.akka.fsm;


import akka.actor.AbstractLoggingFSM;
import akka.actor.ActorRef;

import static com.diorsding.akka.fsm.Messages.Put;
import static com.diorsding.akka.fsm.Messages.Take;


/*
* A chopstick is an actor, it can be taken, and put back
*
* public final void when(S stateName, FSMStateFunctionBuilder<S,D> stateFunctionBuilder)
* public final <E> FSMStateFunctionBuilder<S,D> matchEventEquals(E event, FI.Apply2<E,D,FSM.State<S,D>> apply)
*/
public class Chopstick extends AbstractLoggingFSM<Chopstick.CS, Chopstick.TakenBy> {
    {
        // A chopstick begins its existence as available and taken by no one
        // ActorRef.noSender() vs context().system().deadLetters() Seems both work.
        startWith(CS.Available, new TakenBy(context().system().deadLetters()));

        // When a chopstick is available, it can be taken by a some hakker
        when(CS.Available,
            matchEventEquals(Take, (take, data) ->
                goTo(CS.Taken).using(new TakenBy(sender())).replying(new Messages.Taken(self()))));

        // When a chopstick is taken by a hakker
        // It will refuse to be taken by other hakkers
        // But the owning hakker can put it back
        when(CS.Taken,
            matchEventEquals(Take, (take, data) ->
                stay().replying(new Messages.Busy(self()))).
                event(
                    (event, data) -> (event == Put) && (data.hakker == sender()),
                    (event, data) -> goTo(CS.Available).using(new TakenBy(context().system().deadLetters()))
                ));

        // Initialize the chopstick
        initialize();
    }

    /**
     * Some states the chopstick can be in
     */

    enum CS {
        Available,
        Taken
    }

    /**
     * Some state container for the chopstick
     */

    class TakenBy {
        public final ActorRef hakker;
        public TakenBy(ActorRef hakker){
            this.hakker = hakker;
        }
    }

}
