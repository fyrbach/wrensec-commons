/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.selfservice.example;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.json.resource.Resources.newInternalConnectionFactory;
import static org.forgerock.selfservice.core.ServiceUtils.isEmpty;

import org.forgerock.http.context.RootContext;
import org.forgerock.http.context.ServerContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * Initialises CREST services.
 *
 * @since 0.1.0
 */
public final class CrestInitialiser {

    public ConnectionFactory initialise() throws ResourceException {
        Router router = new Router();
        router.addRoute(Router.uriTemplate("/users"), new MemoryBackend());
        router.addRoute(Router.uriTemplate("/email"), new EmailService());

        ConnectionFactory connectionFactory = newInternalConnectionFactory(router);
        createDemoData(connectionFactory);
        return connectionFactory;
    }

    private void createDemoData(ConnectionFactory connectionFactory) throws ResourceException {
        Connection connection = connectionFactory.getConnection();
        connection.create(new RootContext(),
                newCreateRequest("/users", "1", buildUser("andy123", "Andy", "andy@email.com")));
        connection.create(new RootContext(),
                newCreateRequest("/users", "2", buildUser("peter123", "Peter", "peter@email.com")));
        connection.create(new RootContext(),
                newCreateRequest("/users", "3", buildUser("hannah123", "Hannah", "hannah@email.com")));
    }

    private JsonValue buildUser(String id, String name, String email) {
        return json(
                object(
                        field("_id", id),
                        field("name", name),
                        field("mail", email),
                        field("_rev", "1.0")));
    }

    private static final class EmailService implements SingletonResourceProvider {

        @Override
        public Promise<ActionResponse, ResourceException> actionInstance(ServerContext context, ActionRequest request) {
            if (request.getAction().equals("send")) {
                try {
                    JsonValue response = sendEmail(request.getContent());
                    return Promises.newResultPromise(Responses.newActionResponse(response));
                } catch (ResourceException rE) {
                    return Promises.newExceptionPromise(rE);
                }
            }

            return Promises.newExceptionPromise(
                    ResourceException.newNotSupportedException("Unknown action " + request.getAction()));
        }

        private JsonValue sendEmail(JsonValue document) throws ResourceException {
            String to = document.get("to").asString();

            if (isEmpty(to)) {
                throw new BadRequestException("Field to is not specified");
            }

            String from = document.get("from").asString();

            if (isEmpty(from)) {
                throw new BadRequestException("Field from is not specified");
            }

            String subject = document.get("subject").asString();

            if (isEmpty(subject)) {
                throw new BadRequestException("Field subject is not specified");
            }

            String message = document.get("message").asString();

            if (isEmpty(message)) {
                throw new BadRequestException("Field message is not specified");
            }

            System.out.printf("Sending email to \"%s\" from \"%s\" with subject \"%s\" and message \"%s\".\n",
                    to, from, subject, message);

            return json(
                    object(
                            field("status", "okay")));
        }

        @Override
        public Promise<ResourceResponse, ResourceException> patchInstance(ServerContext context, PatchRequest request) {
            return Promises.newExceptionPromise(ResourceException.newNotSupportedException());
        }

        @Override
        public Promise<ResourceResponse, ResourceException> readInstance(ServerContext context, ReadRequest request) {
            return Promises.newExceptionPromise(ResourceException.newNotSupportedException());
        }

        @Override
        public Promise<ResourceResponse, ResourceException> updateInstance(ServerContext context, UpdateRequest request) {
            return Promises.newExceptionPromise(ResourceException.newNotSupportedException());
        }
    }

}
