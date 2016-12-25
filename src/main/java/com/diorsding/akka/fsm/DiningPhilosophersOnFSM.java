package com.diorsding.akka.fsm;


import akka.actor.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.diorsding.akka.fsm.Messages.Think;


// Akka adaptation of
// http://www.dalnefre.com/wp/2010/08/dining-philosophers-in-humus/

public class DiningPhilosophersOnFSM {

    /*
    * Alright, here's our test-harness
    */
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create();
        //Create 5 chopsticks
        ActorRef[] chopsticks = new ActorRef[5];
        for (int i = 0; i < 5; i++)
            chopsticks[i] = system.actorOf(Props.create(Chopstick.class), "Chopstick" + i);

        //Create 5 awesome hakkers and assign them their left and right chopstick
        List<String> names = Arrays.asList("Ghosh", "Boner", "Klang", "Krasser", "Manie");
        List<ActorRef> hakkers = new ArrayList<>();
        int i = 0;
        for (String name: names) {
            hakkers.add(system.actorOf(Props.create(Philosopher.class, name, chopsticks[i], chopsticks[(i + 1) % 5]), name));
            i++;
        }
        //Signal all hakkers that they should start thinking, and watch the show
        hakkers.stream().forEach(hakker -> hakker.tell(Think, ActorRef.noSender()));
    }
}
