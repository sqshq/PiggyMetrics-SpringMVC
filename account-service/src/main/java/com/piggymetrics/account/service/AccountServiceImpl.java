package com.piggymetrics.account.service;

import com.piggymetrics.account.client.AuthServiceClient;
import com.piggymetrics.account.client.StatisticsServiceClient;
import com.piggymetrics.account.domain.Account;
import com.piggymetrics.account.domain.Currency;
import com.piggymetrics.account.domain.Saving;
import com.piggymetrics.account.domain.User;
import com.piggymetrics.account.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class AccountServiceImpl implements AccountService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private StatisticsServiceClient statisticsClient;
	@Autowired
	private AuthServiceClient authClient;
	@Autowired
	private AccountRepository repository;

	@Override
	public Account findByName(String accountName) {
		Assert.hasLength(accountName);
		Account account = repository.findByName(accountName);
		updateStatistics(account.getName()); // Bad practice: Side effect in a query method
		return account;
	}

	@Override
	public Account create(User user) {
		Account account = checkIfAccountExists(user);
		if (account != null) {
			log.info("account already exists: " + user.getUsername());
			return account;
		}

		authClient.createUser(user);
		Account newAccount = new Account();
		initializeAccount(newAccount, user.getUsername());
		repository.save(newAccount);

		log.info("new account has been created: " + newAccount.getName());
		return newAccount;
	}

	private Account checkIfAccountExists(User user) {
		Account existing = repository.findByName(user.getUsername());
		Assert.isNull(existing, "account already exists: " + user.getUsername());
		return existing;
	}

	private void initializeAccount(Account account, String username) {
		Saving saving = new Saving();
		saving.setAmount(BigDecimal.ZERO); // Using BigDecimal directly
		saving.setCurrency(Currency.getDefault());
		saving.setInterest(BigDecimal.ZERO);
		saving.setDeposit(false);
		saving.setCapitalization(false);

		account.setName(username);
		account.setLastSeen(new Date());
		account.setSaving(saving);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void saveChanges(String name, Account update) {
		Account account = repository.findByName(name);
		Assert.notNull(account, "can't find account with name " + name);

		account.setIncomes(update.getIncomes());
		account.setExpenses(update.getExpenses());
		account.setSaving(update.getSaving());
		account.setNote(update.getNote());
		account.setLastSeen(new Date());

		repository.save(account);
		log.debug("account {} changes has been saved", name);

		statisticsClient.updateStatistics(name, account); // Bad practice: External call within local update logic
	}

	private void updateStatistics(String accountName) {
		statisticsClient.updateStatistics(accountName, repository.findByName(accountName));
	}
}
