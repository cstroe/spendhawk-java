package com.github.cstroe.spendhawk.bean;

import com.github.cstroe.spendhawk.entity.Account;
import com.github.cstroe.spendhawk.entity.CashFlow;
import com.github.cstroe.spendhawk.entity.User;
import com.github.cstroe.spendhawk.util.Ex;
import com.github.cstroe.spendhawk.util.HibernateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Operations on accounts.
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AccountManagerBean extends DatabaseBean {

    private String message;

    private final JanitorBean janitor;

    public String getMessage() {
        return message;
    }

    public Optional<Account> createAccount(Long userId, String accountName) {
        return createAccount(userId, accountName, null);
    }

    public Optional<Account> createAccount(Long userId, String accountName, Long parentId) {
        if(janitor.isBlank(accountName)) {
            message = "Account name cannot be blank.";
            return Optional.empty();
        }

        accountName = janitor.sanitize(accountName);

        try {
            startTransaction();

            final User currentUser = User.findById(userId)
                .orElseThrow(Ex::userNotFound);

            final Account theAccount = new Account();
            theAccount.setName(accountName);
            theAccount.setUser(currentUser);

            if(parentId != null) {
                Account parentAccount = Account.findById(currentUser, parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent account id is not valid."));
                theAccount.setParent(parentAccount);
            }

            HibernateUtil.getSessionFactory().getCurrentSession().save(theAccount);
            commitTransaction();

            return Optional.of(theAccount);
        } catch(Exception ex) {
            rollbackTransaction();
            message = ex.getMessage();
            return Optional.empty();
        }
    }

    public Optional<Account> nestAccount(Long userId, Long parentAccountId, Long subAccountId) {
        try {
            startTransaction();

            User currentUser = User.findById(userId)
                .orElseThrow(Ex::userNotFound);

            final Account parentAccount = Account.findById(currentUser, parentAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Parent account id is not valid."));

            final Account subAccount = Account.findById(currentUser, subAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Sub account id is not valid."));

            subAccount.setParent(parentAccount);

            HibernateUtil.getSessionFactory().getCurrentSession().save(subAccount);
            commitTransaction();

            return Optional.of(subAccount);
        } catch(Exception ex) {
            rollbackTransaction();
            message = ex.getMessage();
            return Optional.empty();
        }
    }

    public boolean deleteAccount(Long userId, Long accountId) {
        try {
            startTransaction();

            User currentUser = User.findById(userId)
                .orElseThrow(Ex::userNotFound);

            Account account = currentUser.getAccounts().stream()
                .filter(a->a.getId().equals(accountId)).findFirst()
                .orElseThrow(Ex::accountNotFound);

            // I have a feeling that this can be handled by hibernate.
            account.getCashFlows().stream()
                .map(CashFlow::getTransaction)
                .collect(Collectors.toSet())
                .forEach(t -> {
                    t.getCashFlows().forEach(CashFlow::delete);
                    t.delete();
                });

            currentUser.getAccounts().remove(account);
            account.delete();
            commitTransaction();
            return true;
        } catch(Exception ex) {
            rollbackTransaction();
            message = Ex.getDescriptiveMessage(ex);
            return false;
        }
    }
}
