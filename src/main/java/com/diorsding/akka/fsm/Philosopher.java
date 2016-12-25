package com.diorsding.akka.fsm;


import akka.actor.AbstractLoggingFSM;
import akka.actor.ActorRef;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;


import static com.diorsding.akka.fsm.Messages.Put;
import static com.diorsding.akka.fsm.Messages.Take;
import static com.diorsding.akka.fsm.Messages.Think;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/*
* A fsm Philosopher is an awesome dude or dudette who either thinks about hacking or has to eat ;-)
*/
public class Philosopher extends AbstractLoggingFSM<Philosopher.HS, Philosopher.TakenChopsticks> {
    private String name;
    private ActorRef left;
    private ActorRef right;

    public Philosopher(String name, ActorRef left, ActorRef right) {
        this.name = name;
        this.left = left;
        this.right = right;
    }

    {
        //All hakkers start waiting
        startWith(HS.Waiting, new TakenChopsticks(null, null));

        when(HS.Waiting,
            matchEventEquals(Think, (think, data) -> {
                System.out.println(String.format("%s starts to think", name));
                return startThinking(Duration.create(5, SECONDS));
            }));

        //When a hakker is thinking it can become hungry
        //and try to pick up its chopsticks and eat
        when(HS.Thinking,
            matchEventEquals(StateTimeout(), (event, data) -> {
                left.tell(Take, self());
                right.tell(Take, self());
                return goTo(HS.Hungry);
            }));

        // When a hakker is hungry it tries to pick up its chopsticks and eat
        // When it picks one up, it goes into wait for the other
        // If the hakkers first attempt at grabbing a chopstick fails,
        // it starts to wait for the response of the other grab
        when(HS.Hungry,
            matchEvent(Messages.Taken.class,
                (taken, data) -> taken.chopstick == left,
                (taken, data) -> goTo(HS.WaitForOtherChopstick).using(new TakenChopsticks(left, null))).
                event(Messages.Taken.class,
                    (taken, data) -> taken.chopstick == right,
                    (taken, data) -> goTo(HS.WaitForOtherChopstick).using(new TakenChopsticks(null, right))).
                event(Messages.Busy.class,
                    (busy, data) -> goTo(HS.FirstChopstickDenied)));

        // When a hakker is waiting for the last chopstick it can either obtain it
        // and start eating, or the other chopstick was busy, and the hakker goes
        // back to think about how he should obtain his chopsticks :-)
        when(HS.WaitForOtherChopstick,
            matchEvent(Messages.Taken.class,
                (taken, data) -> (taken.chopstick == left && data.left == null && data.right != null),
                (taken, data) -> startEating(left, right)).
                event(Messages.Taken.class,
                    (taken, data) -> (taken.chopstick == right && data.left != null && data.right == null),
                    (taken, data) -> startEating(left, right)).
                event(Messages.Busy.class, (busy, data) -> {
                    if (data.left != null) left.tell(Put, self());
                    if (data.right != null) right.tell(Put, self());
                    return startThinking(Duration.create(10, MILLISECONDS));
                }));

        // When the results of the other grab comes back,
        // he needs to put it back if he got the other one.
        // Then go back and think and try to grab the chopsticks again
        when(HS.FirstChopstickDenied,
            matchEvent(Messages.Taken.class,
                (taken, data) -> { taken.chopstick.tell(Put, self());
                return startThinking(Duration.create(10, MILLISECONDS));
            }).event(Messages.Busy.class,
                (busy, data) -> startThinking(Duration.create(10, MILLISECONDS))));

        // When a hakker is eating, he can decide to start to think,
        // then he puts down his chopsticks and starts to think
        when(HS.Eating,
            matchEventEquals(StateTimeout(), (event, data) -> {
                left.tell(Put, self());
                right.tell(Put, self());
                System.out.println(String.format("%s puts down his chopsticks and starts to think", name));
                return startThinking(Duration.create(5, SECONDS));
            }));

        // Initialize the hakker
        initialize();
    }

    private State<HS, TakenChopsticks> startEating(ActorRef left, ActorRef right) {
        System.out.println(String.format("%s has picked up %s and %s and starts to eat",
            name, left.path().name(), right.path().name()));
        return goTo(HS.Eating).using(new TakenChopsticks(left, right)).forMax(Duration.create(5, SECONDS));
    }

    private State<HS, TakenChopsticks> startThinking(FiniteDuration duration) {
        return goTo(HS.Thinking).using(new TakenChopsticks(null, null)).forMax(duration);
    }

    /**
     * Some fsm hakker states
     */
    enum HS {
        Waiting,
        Thinking,
        Hungry,
        WaitForOtherChopstick,
        FirstChopstickDenied,
        Eating
    }

    /**
     * Some state container to keep track of which chopsticks we have
     */
    final class TakenChopsticks {
        public final ActorRef left;
        public final ActorRef right;

        public TakenChopsticks(ActorRef left, ActorRef right) {
            this.left = left;
            this.right = right;
        }
    }
}
