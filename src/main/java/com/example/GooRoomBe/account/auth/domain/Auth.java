package com.example.GooRoomBe.account.auth.domain;

import com.example.GooRoomBe.account.user.domain.User;
import lombok.Getter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.util.UUID;

@Getter
@Node("Auth")
public abstract class Auth {

    @Id @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    protected String id;

    @Relationship(type = "GUARANTEES", direction = Relationship.Direction.OUTGOING)
    protected User user;

    public Auth(User user) {
        this.id = UUID.randomUUID().toString();
        this.user = user;
    }
}