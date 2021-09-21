/*
 * Copyright 2021 ICONation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iconloop.score.token.irc31;

import score.Address;
import score.annotation.Optional;

import java.math.BigInteger;

public interface IRC31 {

    // ================================================
    // External methods
    // ================================================

    /**
     * Returns the balance of the owner's tokens.
     *
     * @param _owner The address of the token holder
     * @param _id    ID of the token
     * @return The _owner's balance of the token type requested
     */
    BigInteger balanceOf(Address _owner, BigInteger _id);

    /**
     * Returns the balance of multiple owner/id pairs.
     *
     * @param _owners The addresses of the token holders
     * @param _ids    IDs of the tokens
     * @return The list of balance (i.e. balance for each owner/id pair)
     */
    BigInteger[] balanceOfBatch(Address[] _owners, BigInteger[] _ids);

    /**
     * Returns an URI for a given token ID.
     *
     * @param _id ID of the token
     * @return The URI string
     */
    String tokenURI(BigInteger _id);

    /**
     * Transfers {@code _value} amount of an token {@code _id} from one address to another address,
     * and must emit {@code TransferSingle} event to reflect the balance change.
     * <p>
     * When the transfer is complete, this method must invoke {@code onIRC31Received} in {@code _to},
     * if {@code _to} is a contract. If the {@code onIRC31Received} method is not implemented in {@code _to} (receiver contract),
     * then the transaction must fail and the transfer of tokens should not occur.
     * If {@code _to} is an externally owned address, then the transaction must be sent without trying to execute
     * {@code onIRC31Received} in {@code _to}.
     * <p>
     * Additional {@code _data} can be attached to this token transaction, and it should be sent unaltered in call
     * to {@code onIRC31Received} in {@code _to}. {@code _data} can be empty.
     * <p>
     * Throws unless the caller is the current token holder or the approved address for the token ID.
     * Throws if {@code _from} does not have enough amount to transfer for the token ID.
     * Throws if {@code _to} is the zero address.
     *
     * @param _from  Source address
     * @param _to    Target address
     * @param _id    ID of the token
     * @param _value The amount of transfer
     * @param _data  Additional data that should be sent unaltered in call to {@code _to}
     */
    void transferFrom(Address _from, Address _to, BigInteger _id, BigInteger _value, @Optional byte[] _data);

    /**
     * Transfers {@code _values} amount(s) of token(s) {@code _ids} from one address to another address,
     * and must emit {@code TransferSingle} or {@code TransferBatch} event(s) to reflect all the balance changes.
     * <p>
     * When all the transfers are complete, this method must invoke {@code onIRC31Received} or
     * {@code onIRC31BatchReceived(Address,Address,int[],int[],bytes)} in {@code _to},
     * if {@code _to} is a contract. If the {@code onIRC31Received} method is not implemented in {@code _to} (receiver contract),
     * then the transaction must fail and the transfers of tokens should not occur.
     * <p>
     * If {@code _to} is an externally owned address, then the transaction must be sent without trying to execute
     * {@code onIRC31Received} in {@code _to}.
     * <p>
     * Additional {@code _data} can be attached to this token transaction, and it should be sent unaltered in call
     * to {@code onIRC31Received} in {@code _to}. {@code _data} can be empty.
     * <p>
     * Throws unless the caller is the current token holder or the approved address for the token IDs.
     * Throws if length of {@code _ids} is not the same as length of {@code _values}.
     * Throws if {@code _from} does not have enough amount to transfer for any of the token IDs.
     * Throws if {@code _to} is the zero address.
     *
     * @param _from   Source address
     * @param _to     Target address
     * @param _ids    IDs of the tokens (order and length must match {@code _values} list)
     * @param _values Transfer amounts per token (order and length must match {@code _ids} list)
     * @param _data   Additional data that should be sent unaltered in call to {@code _to}
     */
    void transferFromBatch(Address _from, Address _to, BigInteger[] _ids, BigInteger[] _values, @Optional byte[] _data);

    /**
     * Enables or disables approval for a third party ("operator") to manage all of the caller's tokens,
     * and must emit {@code ApprovalForAll} event on success.
     *
     * @param _operator Address to add to the set of authorized operators
     * @param _approved True if the operator is approved, false to revoke approval
     */
    void setApprovalForAll(Address _operator, boolean _approved);

    /**
     * Returns the approval status of an operator for a given owner.
     *
     * @param _owner    The owner of the tokens
     * @param _operator The address of authorized operator
     * @return True if the operator is approved, false otherwise
     */
    boolean isApprovedForAll(Address _owner, Address _operator);

    // ================================================
    // Event Logs
    // ================================================

    /**
     * Must trigger on any successful token transfers, including zero value transfers as well as minting or burning.
     * When minting/creating tokens, the {@code _from} must be set to zero address.
     * When burning/destroying tokens, the {@code _to} must be set to zero address.
     *
     * @param _operator The address of an account/contract that is approved to make the transfer
     * @param _from     The address of the token holder whose balance is decreased
     * @param _to       The address of the recipient whose balance is increased
     * @param _id       ID of the token
     * @param _value    The amount of transfer
     */
    void TransferSingle(Address _operator, Address _from, Address _to, BigInteger _id, BigInteger _value);

    /**
     * Must trigger on any successful token transfers, including zero value transfers as well as minting or burning.
     * When minting/creating tokens, the {@code _from} must be set to zero address.
     * When burning/destroying tokens, the {@code _to} must be set to zero address.
     * <p>
     * NOTE: RLP (Recursive Length Prefix) would be used for the serialized bytes to represent list type.
     *
     * @param _operator The address of an account/contract that is approved to make the transfer
     * @param _from     The address of the token holder whose balance is decreased
     * @param _to       The address of the recipient whose balance is increased
     * @param _ids      Serialized bytes of list for token IDs (order and length must match {@code _values})
     * @param _values   Serialized bytes of list for transfer amounts per token (order and length must match {@code _ids})
     */
    void TransferBatch(Address _operator, Address _from, Address _to, byte[] _ids, byte[] _values);

    /**
     * Must trigger on any successful approval (either enabled or disabled) for a third party/operator address
     * to manage all tokens for the {@code _owner} address.
     *
     * @param _owner    The address of the token holder
     * @param _operator The address of authorized operator
     * @param _approved True if the operator is approved, false to revoke approval
     */
    void ApprovalForAll(Address _owner, Address _operator, boolean _approved);

    /**
     * Must trigger on any successful URI updates for a token ID.
     * URIs are defined in RFC 3986.
     * The URI must point to a JSON file that conforms to the "ERC-1155 Metadata URI JSON Schema".
     *
     * @param _id    ID of the token
     * @param _value The updated URI string
     */
    void URI(BigInteger _id, String _value);
}
