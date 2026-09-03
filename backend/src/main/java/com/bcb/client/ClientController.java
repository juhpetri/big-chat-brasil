package com.bcb.client;

import com.bcb.auth.AuthenticatedClient;
import com.bcb.client.dto.AddCreditRequest;
import com.bcb.client.dto.AdjustLimitRequest;
import com.bcb.client.dto.ClientResponse;
import com.bcb.client.dto.ConvertPlanRequest;
import com.bcb.client.dto.CreateClientRequest;
import com.bcb.client.exceptions.ClientNotFoundException;
import com.bcb.common.ErrorResponse;
import com.bcb.transaction.TransactionService;
import com.bcb.transaction.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Clientes", description = "Cadastro de clientes e gestão de plano, saldo/limite e extrato de transações")
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final TransactionService transactionService;
    private final AuthenticatedClient authenticatedClient;

    @Operation(summary = "Cadastrar cliente",
            description = "Cria um cliente PREPAID (com saldo inicial) ou POSTPAID (com limite mensal inicial). "
                    + "Não requer autenticação.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente criado",
                    content = @Content(schema = @Schema(implementation = ClientResponse.class))),
            @ApiResponse(responseCode = "400", description = "Documento em formato inválido ou payload inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Já existe cliente cadastrado com esse documento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirements
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse createClient(@Valid @RequestBody CreateClientRequest clientRequest) {
        return clientService.createClient(clientRequest);
    }

    @Operation(summary = "Buscar cliente por id", description = "Só o próprio cliente autenticado pode consultar seus dados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado",
                    content = @Content(schema = @Schema(implementation = ClientResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado, ou id não pertence ao cliente autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ClientResponse getClientById(@Parameter(description = "Id do cliente") @PathVariable UUID id) {
        assertSelf(id);
        return clientService.getClientById(id);
    }

    @Operation(summary = "Adicionar crédito", description = "Soma o valor informado ao saldo do cliente. Só válido para clientes PREPAID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Crédito adicionado",
                    content = @Content(schema = @Schema(implementation = ClientResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido ou cliente não é PREPAID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/credit")
    @ResponseStatus(HttpStatus.OK)
    public ClientResponse addCredit(@Parameter(description = "Id do cliente") @PathVariable UUID id,
                                     @Valid @RequestBody AddCreditRequest request) {
        assertSelf(id);
        return clientService.addCredit(id, request.amount());
    }

    @Operation(summary = "Ajustar limite mensal", description = "Define um novo limite mensal. Só válido para clientes POSTPAID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Limite ajustado",
                    content = @Content(schema = @Schema(implementation = ClientResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido ou cliente não é POSTPAID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/limit")
    @ResponseStatus(HttpStatus.OK)
    public ClientResponse adjustLimit(@Parameter(description = "Id do cliente") @PathVariable UUID id,
                                       @Valid @RequestBody AdjustLimitRequest request) {
        assertSelf(id);
        return clientService.adjustLimit(id, request.newLimit());
    }

    @Operation(summary = "Converter plano", description = "Converte o cliente entre PREPAID e POSTPAID, "
            + "registrando o residual do plano anterior como transação de fechamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plano convertido",
                    content = @Content(schema = @Schema(implementation = ClientResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido ou cliente já está no plano informado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/plan")
    @ResponseStatus(HttpStatus.OK)
    public ClientResponse convertPlan(@Parameter(description = "Id do cliente") @PathVariable UUID id,
                                       @Valid @RequestBody ConvertPlanRequest request) {
        assertSelf(id);
        return clientService.convertPlan(id, request.newPlanType(), request.initialValue());
    }

    @Operation(summary = "Listar transações do cliente", description = "Extrato de débitos e créditos, mais recente primeiro. "
            + "Só o próprio cliente autenticado pode consultar seu extrato.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de transações (pode ser vazia)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Id não pertence ao cliente autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/transactions")
    @ResponseStatus(HttpStatus.OK)
    public List<TransactionResponse> transactions(@Parameter(description = "Id do cliente") @PathVariable UUID id) {
        assertSelf(id);
        return transactionService.listByClient(id);
    }

    private void assertSelf(UUID id) {
        if (!authenticatedClient.getClientId().equals(id)) {
            throw new ClientNotFoundException(id);
        }
    }
}
