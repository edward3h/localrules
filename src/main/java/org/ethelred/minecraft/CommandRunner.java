package org.ethelred.minecraft;

import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;

import javax.validation.constraints.NotBlank;

@Client("${command.url}")
@Debug
public interface CommandRunner {
    @Post
    CommandResponse runCommand(@NotBlank String command, String token);

    record CommandResponse(int status, String response) {
    }
}
