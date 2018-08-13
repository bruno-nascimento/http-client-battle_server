package br.com.labbs.workout.httpclientbattleserver.infra;

import akka.actor.ActorSystem;

public enum Akka {

    INSTANCE;

    private ActorSystem system = ActorSystem.create("http-client-battle_server");

    public ActorSystem getSystem() {
        return system;
    }

}
