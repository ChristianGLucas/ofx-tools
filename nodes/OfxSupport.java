package nodes;

import com.webcohesion.ofx4j.domain.data.MessageSetType;
import com.webcohesion.ofx4j.domain.data.ResponseEnvelope;
import com.webcohesion.ofx4j.domain.data.ResponseMessageSet;
import com.webcohesion.ofx4j.domain.data.banking.BankAccountDetails;
import com.webcohesion.ofx4j.domain.data.banking.BankStatementResponse;
import com.webcohesion.ofx4j.domain.data.banking.BankStatementResponseTransaction;
import com.webcohesion.ofx4j.domain.data.banking.BankingResponseMessageSet;
import com.webcohesion.ofx4j.domain.data.common.BalanceInfo;
import com.webcohesion.ofx4j.domain.data.common.Payee;
import com.webcohesion.ofx4j.domain.data.common.Status;
import com.webcohesion.ofx4j.domain.data.common.Transaction;
import com.webcohesion.ofx4j.domain.data.common.TransactionList;
import com.webcohesion.ofx4j.domain.data.creditcard.CreditCardAccountDetails;
import com.webcohesion.ofx4j.domain.data.creditcard.CreditCardResponseMessageSet;
import com.webcohesion.ofx4j.domain.data.creditcard.CreditCardStatementResponse;
import com.webcohesion.ofx4j.domain.data.creditcard.CreditCardStatementResponseTransaction;
import com.webcohesion.ofx4j.domain.data.investment.accounts.InvestmentAccountDetails;
import com.webcohesion.ofx4j.domain.data.investment.statements.InvestmentStatementResponse;
import com.webcohesion.ofx4j.domain.data.investment.statements.InvestmentStatementResponseMessageSet;
import com.webcohesion.ofx4j.domain.data.investment.statements.InvestmentStatementResponseTransaction;
import com.webcohesion.ofx4j.domain.data.investment.transactions.BaseBuyInvestmentTransaction;
import com.webcohesion.ofx4j.domain.data.investment.transactions.BaseInvestmentTransaction;
import com.webcohesion.ofx4j.domain.data.investment.transactions.BaseSellInvestmentTransaction;
import com.webcohesion.ofx4j.domain.data.investment.transactions.IncomeTransaction;
import com.webcohesion.ofx4j.domain.data.investment.transactions.InvestmentTransactionList;
import com.webcohesion.ofx4j.domain.data.seclist.SecurityId;
import com.webcohesion.ofx4j.domain.data.signon.FinancialInstitution;
import com.webcohesion.ofx4j.domain.data.signon.SignonResponse;
import com.webcohesion.ofx4j.io.AggregateUnmarshaller;
import com.webcohesion.ofx4j.io.DefaultStringConversion;
import com.webcohesion.ofx4j.io.OFXParseException;

import gen.Messages;

import java.io.StringReader;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared parsing/conversion logic for every node in this package. Not a node
 * itself — package-private helpers only.
 *
 * <p>Security posture (see README / retrospective for the full reasoning):
 * every entry point here bounds input size, rejects any DOCTYPE declaration
 * before it ever reaches ofx4j's SAX-based OFX-2/XML reader (which does not
 * disable external entity resolution on its own — this is our XXE guard),
 * and converts any parse failure (including a StackOverflowError from
 * adversarially deep SGML nesting) into a structured OfxError rather than
 * letting it propagate.
 */
final class OfxSupport {

  private OfxSupport() {
  }

  static final int MAX_INPUT_CHARS = 5_000_000;
  static final int MAX_DATE_INPUT_CHARS = 64;

  private static final Pattern OFX2_PROCESSING_INSTRUCTION =
      Pattern.compile("<\\?OFX\\s+([^\\?]+)\\?>", Pattern.CASE_INSENSITIVE);
  private static final Pattern VERSION_ATTRIBUTE =
      Pattern.compile("VERSION\\s*=\\s*\"?([0-9]+)\"?", Pattern.CASE_INSENSITIVE);
  private static final Pattern SGML_VERSION_HEADER_LINE =
      Pattern.compile("(?im)^\\s*VERSION\\s*:\\s*([0-9]+)\\s*$");
  private static final Pattern OFX_ROOT_TAG = Pattern.compile("(?i)<OFX[\\s>]");

  // JDK-standard ISO-8601 instant formatter — used both to format our own
  // dates and (via java.time.Instant.parse, which is spec-compatible with
  // it) to parse caller-supplied ISO instant bounds back.
  private static final DateTimeFormatter ISO_INSTANT_FORMAT = DateTimeFormatter.ISO_INSTANT;

  // ─────────────────────────── error helpers ───────────────────────────

  static Messages.OfxError okError() {
    return Messages.OfxError.newBuilder().setOk(true).build();
  }

  static Messages.OfxError error(String code, String message) {
    return Messages.OfxError.newBuilder().setOk(false).setCode(code).setMessage(message).build();
  }

  private static String safeMessage(Throwable t) {
    String m = t.getMessage();
    if (m == null || m.isEmpty()) {
      m = t.getClass().getSimpleName();
    }
    if (m.length() > 500) {
      m = m.substring(0, 500) + "...";
    }
    return m;
  }

  /**
   * Validates raw input against the shared safety bounds. Returns null when
   * the input is safe to parse further, or a structured error explaining
   * why it was rejected.
   */
  static Messages.OfxError guardInput(String text) {
    if (text == null || text.trim().isEmpty()) {
      return error("EMPTY_INPUT", "ofx_text is empty.");
    }
    if (text.length() > MAX_INPUT_CHARS) {
      return error("TOO_LARGE",
          "ofx_text is " + text.length() + " characters, exceeding the " + MAX_INPUT_CHARS + " character limit.");
    }
    // OFX documents never legitimately declare a DOCTYPE. ofx4j's OFX-2/XML
    // reader uses a bare org.xml.sax.helpers.XMLReaderFactory.createXMLReader()
    // with no external-entity/DTD hardening, so we reject any DOCTYPE outright
    // rather than ever handing it to that parser (this is the XXE guard).
    if (text.toLowerCase(Locale.ROOT).contains("<!doctype")) {
      return error("UNSAFE_DOCTYPE",
          "Input contains a DOCTYPE declaration, which is rejected to prevent XXE. OFX documents never legitimately need one.");
    }
    return null;
  }

  // ─────────────────────────── format detection ───────────────────────────

  /** Header-only detection: never touches the document body. */
  static Messages.FormatInfo detectFormat(String ofxText) {
    Messages.OfxError guardErr = guardInput(ofxText);
    if (guardErr != null) {
      return Messages.FormatInfo.newBuilder().setError(guardErr).build();
    }

    Matcher pi = OFX2_PROCESSING_INSTRUCTION.matcher(ofxText);
    Matcher rootTag = OFX_ROOT_TAG.matcher(ofxText);
    boolean hasPi = pi.find();
    boolean hasRoot = rootTag.find();

    if (hasPi && (!hasRoot || pi.start() < rootTag.start())) {
      String version = "";
      Matcher vm = VERSION_ATTRIBUTE.matcher(pi.group(1));
      if (vm.find()) {
        version = vm.group(1);
      }
      return Messages.FormatInfo.newBuilder()
          .setSyntax(Messages.OfxSyntax.XML)
          .setVersion(version)
          .setError(okError())
          .build();
    }

    if (hasRoot) {
      String header = ofxText.substring(0, rootTag.start());
      String version = "";
      Matcher vm = SGML_VERSION_HEADER_LINE.matcher(header);
      if (vm.find()) {
        version = vm.group(1);
      }
      return Messages.FormatInfo.newBuilder()
          .setSyntax(Messages.OfxSyntax.SGML)
          .setVersion(version)
          .setError(okError())
          .build();
    }

    return Messages.FormatInfo.newBuilder()
        .setError(error("NO_ROOT_OFX", "No root <OFX> element found in the input."))
        .build();
  }

  // ─────────────────────────── full parse ───────────────────────────

  static final class ParseOutcome {
    ResponseEnvelope env;
    String version = "";
    Messages.OfxSyntax syntax = Messages.OfxSyntax.OFX_SYNTAX_UNSPECIFIED;
    Messages.OfxError error;
  }

  static ParseOutcome parse(String ofxText) {
    ParseOutcome outcome = new ParseOutcome();

    Messages.OfxError guardErr = guardInput(ofxText);
    if (guardErr != null) {
      outcome.error = guardErr;
      return outcome;
    }

    Messages.FormatInfo fi = detectFormat(ofxText);
    if (!fi.getError().getOk()) {
      outcome.error = fi.getError();
      return outcome;
    }
    outcome.version = fi.getVersion();
    outcome.syntax = fi.getSyntax();

    try {
      AggregateUnmarshaller<ResponseEnvelope> unmarshaller = new AggregateUnmarshaller<>(ResponseEnvelope.class);
      outcome.env = unmarshaller.unmarshal(new StringReader(ofxText));
    } catch (OFXParseException e) {
      outcome.error = error("MALFORMED", "Failed to parse OFX document: " + safeMessage(e));
    } catch (Throwable t) {
      // Deliberately broad: a malformed real-world OFX file can trigger an
      // unchecked exception deep in ofx4j (e.g. a required field missing),
      // or — for adversarially deep SGML aggregate nesting — a
      // StackOverflowError from the recursive descent in
      // NanoXMLOFXReader#processOFXTag. Both are safe to catch in pure JVM
      // code (no native frames involved) and must become a structured
      // error, never an uncaught crash.
      outcome.error = error("MALFORMED", "Failed to parse OFX document: " + safeMessage(t));
    }
    return outcome;
  }

  static int countStatements(ResponseEnvelope env) {
    int count = 0;
    SortedSet<ResponseMessageSet> sets = env.getMessageSets();
    if (sets == null) {
      return 0;
    }
    for (ResponseMessageSet ms : sets) {
      if (ms instanceof BankingResponseMessageSet) {
        List<BankStatementResponseTransaction> l = ((BankingResponseMessageSet) ms).getStatementResponses();
        count += l == null ? 0 : l.size();
      } else if (ms instanceof CreditCardResponseMessageSet) {
        List<CreditCardStatementResponseTransaction> l = ((CreditCardResponseMessageSet) ms).getStatementResponses();
        count += l == null ? 0 : l.size();
      } else if (ms instanceof InvestmentStatementResponseMessageSet) {
        List<InvestmentStatementResponseTransaction> l = ((InvestmentStatementResponseMessageSet) ms).getStatementResponses();
        count += l == null ? 0 : l.size();
      }
    }
    return count;
  }

  // ─────────────────────────── document conversion ───────────────────────────

  static Messages.OfxDocument buildOfxDocument(String ofxText) {
    ParseOutcome outcome = parse(ofxText);
    Messages.OfxDocument.Builder doc = Messages.OfxDocument.newBuilder();
    if (outcome.error != null) {
      return doc.setError(outcome.error).build();
    }

    doc.setVersion(outcome.version);
    doc.setSyntax(outcome.syntax);
    doc.setError(okError());

    ResponseEnvelope env = outcome.env;

    SignonResponse signon = env.getSignonResponse();
    if (signon != null) {
      Messages.SignOnInfo.Builder si = Messages.SignOnInfo.newBuilder();
      Status status = signon.getStatus();
      if (status != null) {
        if (status.getCode() != null) {
          si.setStatusCode(String.valueOf(status.getCode()));
        }
        if (status.getSeverity() != null) {
          si.setStatusSeverity(status.getSeverity().name());
        }
        if (status.getMessage() != null) {
          si.setStatusMessage(status.getMessage());
        }
      }
      si.setServerDateIso(toIso(signon.getTimestamp()));
      if (signon.getLanguage() != null) {
        si.setLanguage(signon.getLanguage());
      }
      FinancialInstitution fi = signon.getFinancialInstitution();
      if (fi != null) {
        if (fi.getOrganization() != null) {
          si.setOrg(fi.getOrganization());
        }
        if (fi.getId() != null) {
          si.setFid(fi.getId());
        }
      }
      if (signon.getSessionId() != null) {
        si.setSessionId(signon.getSessionId());
      }
      doc.setSignOn(si.build());
    }

    SortedSet<ResponseMessageSet> messageSets = env.getMessageSets();
    if (messageSets != null) {
      for (ResponseMessageSet ms : messageSets) {
        if (ms instanceof BankingResponseMessageSet) {
          List<BankStatementResponseTransaction> list = ((BankingResponseMessageSet) ms).getStatementResponses();
          if (list != null) {
            for (BankStatementResponseTransaction t : list) {
              BankStatementResponse resp = t.getMessage();
              if (resp != null) {
                doc.addStatements(buildBankStatement(resp));
              }
            }
          }
        } else if (ms instanceof CreditCardResponseMessageSet) {
          List<CreditCardStatementResponseTransaction> list =
              ((CreditCardResponseMessageSet) ms).getStatementResponses();
          if (list != null) {
            for (CreditCardStatementResponseTransaction t : list) {
              CreditCardStatementResponse resp = t.getMessage();
              if (resp != null) {
                doc.addStatements(buildCreditCardStatement(resp));
              }
            }
          }
        } else if (ms instanceof InvestmentStatementResponseMessageSet) {
          List<InvestmentStatementResponseTransaction> list =
              ((InvestmentStatementResponseMessageSet) ms).getStatementResponses();
          if (list != null) {
            for (InvestmentStatementResponseTransaction t : list) {
              InvestmentStatementResponse resp = t.getMessage();
              if (resp != null) {
                doc.addStatements(buildInvestmentStatement(resp));
              }
            }
          }
        }
        // Other message sets (signup, payments, email, profile, tax1099,
        // interbank/wire transfer) are session/service data, not statement
        // data, and are intentionally out of scope for this package.
      }
    }

    return doc.build();
  }

  private static Messages.Statement buildBankStatement(BankStatementResponse resp) {
    Messages.Statement.Builder st = Messages.Statement.newBuilder().setStatementKind("BANK");

    BankAccountDetails acct = resp.getAccount();
    String acctId = "";
    Messages.Account.Builder accB = Messages.Account.newBuilder();
    if (acct != null) {
      if (acct.getBankId() != null) {
        accB.setBankId(acct.getBankId());
      }
      if (acct.getBranchId() != null) {
        accB.setBranchId(acct.getBranchId());
      }
      if (acct.getAccountNumber() != null) {
        acctId = acct.getAccountNumber();
        accB.setAccountId(acctId);
      }
      accB.setAccountType(mapBankAccountType(acct.getAccountType()));
      if (acct.getAccountKey() != null) {
        accB.setAccountKey(acct.getAccountKey());
      }
    }
    st.setAccount(accB.build());

    if (resp.getCurrencyCode() != null) {
      st.setCurrency(resp.getCurrencyCode());
    }

    TransactionList txl = resp.getTransactionList();
    if (txl != null) {
      st.setStartDateIso(toIso(txl.getStart()));
      st.setEndDateIso(toIso(txl.getEnd()));
      if (txl.getTransactions() != null) {
        for (Transaction t : txl.getTransactions()) {
          st.addTransactions(buildTransaction(t, acctId, "BANK"));
        }
      }
    }

    st.setLedgerBalance(buildBalance(resp.getLedgerBalance()));
    st.setAvailableBalance(buildBalance(resp.getAvailableBalance()));
    return st.build();
  }

  private static Messages.Statement buildCreditCardStatement(CreditCardStatementResponse resp) {
    Messages.Statement.Builder st = Messages.Statement.newBuilder().setStatementKind("CREDITCARD");

    CreditCardAccountDetails acct = resp.getAccount();
    String acctId = "";
    Messages.Account.Builder accB = Messages.Account.newBuilder().setAccountType(Messages.AccountType.CREDITCARD);
    if (acct != null) {
      if (acct.getAccountNumber() != null) {
        acctId = acct.getAccountNumber();
        accB.setAccountId(acctId);
      }
      if (acct.getAccountKey() != null) {
        accB.setAccountKey(acct.getAccountKey());
      }
    }
    st.setAccount(accB.build());

    if (resp.getCurrencyCode() != null) {
      st.setCurrency(resp.getCurrencyCode());
    }

    TransactionList txl = resp.getTransactionList();
    if (txl != null) {
      st.setStartDateIso(toIso(txl.getStart()));
      st.setEndDateIso(toIso(txl.getEnd()));
      if (txl.getTransactions() != null) {
        for (Transaction t : txl.getTransactions()) {
          st.addTransactions(buildTransaction(t, acctId, "CREDITCARD"));
        }
      }
    }

    st.setLedgerBalance(buildBalance(resp.getLedgerBalance()));
    st.setAvailableBalance(buildBalance(resp.getAvailableBalance()));
    return st.build();
  }

  private static Messages.Statement buildInvestmentStatement(InvestmentStatementResponse resp) {
    Messages.Statement.Builder st = Messages.Statement.newBuilder().setStatementKind("INVESTMENT");

    InvestmentAccountDetails acct = resp.getAccount();
    String acctId = "";
    Messages.Account.Builder accB = Messages.Account.newBuilder().setAccountType(Messages.AccountType.INVESTMENT);
    if (acct != null) {
      if (acct.getBrokerId() != null) {
        accB.setBankId(acct.getBrokerId());
      }
      if (acct.getAccountNumber() != null) {
        acctId = acct.getAccountNumber();
        accB.setAccountId(acctId);
      }
      if (acct.getAccountKey() != null) {
        accB.setAccountKey(acct.getAccountKey());
      }
    }
    st.setAccount(accB.build());

    if (resp.getCurrencyCode() != null) {
      st.setCurrency(resp.getCurrencyCode());
    }

    InvestmentTransactionList itl = resp.getInvestmentTransactionList();
    if (itl != null) {
      st.setStartDateIso(toIso(itl.getStart()));
      st.setEndDateIso(toIso(itl.getEnd()));
      if (itl.getInvestmentTransactions() != null) {
        for (BaseInvestmentTransaction t : itl.getInvestmentTransactions()) {
          st.addInvestmentTransactions(buildInvestmentTransaction(t, acctId));
        }
      }
    } else {
      st.setEndDateIso(toIso(resp.getDateOfStatement()));
    }

    // Investment balances (available cash / margin / short / buying power)
    // are a fundamentally different shape than the bank/credit-card
    // ledger+available balance pair, so ledger_balance/available_balance
    // are intentionally left absent (present=false) here rather than
    // force-fit — see the retrospective for the recorded scope decision.
    return st.build();
  }

  private static Messages.Transaction buildTransaction(Transaction t, String acctId, String statementKind) {
    Messages.Transaction.Builder tb = Messages.Transaction.newBuilder()
        .setAccountId(acctId == null ? "" : acctId)
        .setStatementKind(statementKind);

    if (t.getTransactionType() != null) {
      tb.setType(t.getTransactionType().name());
    }
    tb.setDatePostedIso(toIso(t.getDatePosted()));
    tb.setDateUserIso(toIso(t.getDateInitiated()));
    Double amount = t.getAmount();
    if (amount != null) {
      tb.setAmount(amount);
    }
    if (t.getId() != null) {
      tb.setFitid(t.getId());
    }
    if (t.getCheckNumber() != null) {
      tb.setCheckNum(t.getCheckNumber());
    }
    if (t.getReferenceNumber() != null) {
      tb.setRefNum(t.getReferenceNumber());
    }
    String name = t.getName();
    if ((name == null || name.isEmpty()) && t.getPayee() != null) {
      Payee payee = t.getPayee();
      if (payee.getName() != null) {
        name = payee.getName();
      }
    }
    if (name != null) {
      tb.setName(name);
    }
    if (t.getMemo() != null) {
      tb.setMemo(t.getMemo());
    }
    if (t.getStandardIndustrialCode() != null) {
      tb.setSic(t.getStandardIndustrialCode());
    }
    return tb.build();
  }

  private static Messages.Balance buildBalance(BalanceInfo bal) {
    Messages.Balance.Builder b = Messages.Balance.newBuilder();
    if (bal == null) {
      return b.setPresent(false).build();
    }
    b.setPresent(true);
    b.setAmount(bal.getAmount());
    b.setAsOfDateIso(toIso(bal.getAsOfDate()));
    return b.build();
  }

  private static Messages.AccountType mapBankAccountType(com.webcohesion.ofx4j.domain.data.banking.AccountType t) {
    if (t == null) {
      return Messages.AccountType.ACCOUNT_TYPE_UNSPECIFIED;
    }
    switch (t) {
      case CHECKING:
        return Messages.AccountType.CHECKING;
      case SAVINGS:
        return Messages.AccountType.SAVINGS;
      case MONEYMRKT:
        return Messages.AccountType.MONEYMRKT;
      case CREDITLINE:
        return Messages.AccountType.CREDITLINE;
      default:
        return Messages.AccountType.ACCOUNT_TYPE_UNSPECIFIED;
    }
  }

  private static Messages.InvestmentTransaction buildInvestmentTransaction(BaseInvestmentTransaction bt, String acctId) {
    Messages.InvestmentTransaction.Builder ib = Messages.InvestmentTransaction.newBuilder()
        .setAccountId(acctId == null ? "" : acctId);

    if (bt.getTransactionType() != null) {
      ib.setType(bt.getTransactionType().name());
    }
    if (bt.getTransactionId() != null) {
      ib.setFitid(bt.getTransactionId());
    }
    ib.setTradeDateIso(toIso(bt.getTradeDate()));
    ib.setSettleDateIso(toIso(bt.getSettlementDate()));
    if (bt.getMemo() != null) {
      ib.setMemo(bt.getMemo());
    }

    // Full monetary detail (units/price/commission/fees/total/security) is
    // populated for the buy/sell/income families, which cover the large
    // majority of real investment activity and share a uniform accessor
    // shape in ofx4j. Other investment transaction types (splits,
    // transfers, margin interest, journal entries, reinvestment, return of
    // capital, option closures) report only the common fields above —
    // ofx4j does not expose a uniform typed accessor for their
    // type-specific fields, and per-subtype casts for each of the ~10
    // remaining concrete classes were left out of this version (see the
    // retrospective).
    if (bt instanceof BaseBuyInvestmentTransaction) {
      BaseBuyInvestmentTransaction buy = (BaseBuyInvestmentTransaction) bt;
      setSecurity(ib, buy.getSecurityId());
      if (buy.getUnits() != null) {
        ib.setUnits(buy.getUnits());
      }
      if (buy.getUnitPrice() != null) {
        ib.setUnitPrice(buy.getUnitPrice());
      }
      if (buy.getCommission() != null) {
        ib.setCommission(buy.getCommission());
      }
      if (buy.getFees() != null) {
        ib.setFees(buy.getFees());
      }
      if (buy.getTotal() != null) {
        ib.setTotal(buy.getTotal());
      }
      if (buy.getSubAccountSecurity() != null) {
        ib.setSubAccount(buy.getSubAccountSecurity());
      }
    } else if (bt instanceof BaseSellInvestmentTransaction) {
      BaseSellInvestmentTransaction sell = (BaseSellInvestmentTransaction) bt;
      setSecurity(ib, sell.getSecurityId());
      if (sell.getUnits() != null) {
        ib.setUnits(sell.getUnits());
      }
      if (sell.getUnitPrice() != null) {
        ib.setUnitPrice(sell.getUnitPrice());
      }
      if (sell.getCommission() != null) {
        ib.setCommission(sell.getCommission());
      }
      if (sell.getFees() != null) {
        ib.setFees(sell.getFees());
      }
      if (sell.getTotal() != null) {
        ib.setTotal(sell.getTotal());
      }
      if (sell.getSubAccountSecurity() != null) {
        ib.setSubAccount(sell.getSubAccountSecurity());
      }
    } else if (bt instanceof IncomeTransaction) {
      IncomeTransaction inc = (IncomeTransaction) bt;
      setSecurity(ib, inc.getSecurityId());
      if (inc.getTotal() != null) {
        ib.setTotal(inc.getTotal());
      }
      if (inc.getIncomeType() != null) {
        ib.setIncomeType(inc.getIncomeType());
      }
      if (inc.getSubAccountSecurity() != null) {
        ib.setSubAccount(inc.getSubAccountSecurity());
      }
    }

    return ib.build();
  }

  private static void setSecurity(Messages.InvestmentTransaction.Builder ib, SecurityId sid) {
    if (sid == null) {
      return;
    }
    if (sid.getUniqueId() != null) {
      ib.setSecurityId(sid.getUniqueId());
    }
    if (sid.getUniqueIdType() != null) {
      ib.setSecurityIdType(sid.getUniqueIdType());
    }
  }

  // ─────────────────────────── structural validation ───────────────────────────

  static Messages.ValidateStructureResult validateStructure(String ofxText) {
    Messages.OfxError guardErr = guardInput(ofxText);
    if (guardErr != null) {
      return Messages.ValidateStructureResult.newBuilder().setError(guardErr).setValid(false).build();
    }

    ParseOutcome outcome = parse(ofxText);
    Messages.ValidateStructureResult.Builder r = Messages.ValidateStructureResult.newBuilder().setError(okError());
    List<String> issues = new ArrayList<>();

    if (outcome.error != null) {
      issues.add(outcome.error.getMessage());
      r.setHasOfxRoot(false).setHasSignon(false).setStatementCount(0);
    } else {
      r.setHasOfxRoot(true);
      boolean hasSignon = outcome.env.getSignonResponse() != null;
      r.setHasSignon(hasSignon);
      if (!hasSignon) {
        issues.add("missing SIGNONMSGSRSV1/SONRS sign-on response");
      }
      int stmtCount = countStatements(outcome.env);
      r.setStatementCount(stmtCount);
      if (stmtCount == 0) {
        issues.add("no statement response found (no bank, credit-card, or investment statement)");
      }
    }

    r.setValid(issues.isEmpty());
    r.addAllIssues(issues);
    return r.build();
  }

  /**
   * Lenient bound parser for date-range filtering: accepts either a
   * date-only ISO string ("yyyy-MM-dd") or a full UTC ISO instant
   * ("yyyy-MM-dd'T'HH:mm:ss'Z'"), and compares as epoch millis rather than
   * lexicographically — a naive string comparison between a date-only
   * bound and a full-instant transaction date silently mishandles the
   * boundary day (e.g. an end bound of "2025-07-14" would lexicographically
   * exclude every transaction ON July 14th). A date-only END bound is
   * treated as inclusive through the end of that UTC day; a date-only
   * START bound is treated as the start of that UTC day. An unparseable
   * bound returns null and is treated by the caller as "no bound" (ignored,
   * not an error) — a malformed filter bound should never crash this node.
   */
  static Long parseIsoBoundEpochMillis(String s, boolean endOfDayIfDateOnly) {
    if (s == null || s.trim().isEmpty()) {
      return null;
    }
    String v = s.trim();
    try {
      if (v.length() == 10) {
        java.time.LocalDate date = java.time.LocalDate.parse(v);
        java.time.Instant instant = endOfDayIfDateOnly
            ? date.atTime(23, 59, 59).toInstant(ZoneOffset.UTC)
            : date.atStartOfDay(ZoneOffset.UTC).toInstant();
        return instant.toEpochMilli();
      }
      return java.time.Instant.parse(v).toEpochMilli();
    } catch (Exception e) {
      return null;
    }
  }

  // ─────────────────────────── shared utils ───────────────────────────

  static String toIso(Date d) {
    if (d == null) {
      return "";
    }
    return ISO_INSTANT_FORMAT.format(d.toInstant());
  }

  /** Exposes ofx4j's own (protected) OFX date grammar for standalone use. */
  private static final class ExposedDateConversion extends DefaultStringConversion {
    Date parsePublic(String value) {
      return parseDate(value);
    }
  }

  /**
   * Parses a bare OFX date/time string using ofx4j's own date grammar and
   * normalizes it to ISO 8601 UTC. Returns a structured error rather than
   * throwing on malformed input.
   */
  static Messages.NormalizeDateResult normalizeDate(String rawOfxDate) {
    if (rawOfxDate == null || rawOfxDate.trim().isEmpty()) {
      return Messages.NormalizeDateResult.newBuilder()
          .setError(error("EMPTY_INPUT", "ofx_date is empty."))
          .build();
    }
    if (rawOfxDate.length() > MAX_DATE_INPUT_CHARS) {
      return Messages.NormalizeDateResult.newBuilder()
          .setError(error("TOO_LARGE", "ofx_date exceeds the " + MAX_DATE_INPUT_CHARS + " character limit."))
          .build();
    }
    try {
      Date parsed = new ExposedDateConversion().parsePublic(rawOfxDate);
      if (parsed == null) {
        return Messages.NormalizeDateResult.newBuilder()
            .setError(error("MALFORMED", "Could not parse ofx_date: " + rawOfxDate))
            .build();
      }
      String datePrefix = rawOfxDate.contains("[") ? rawOfxDate.substring(0, rawOfxDate.indexOf('[')) : rawOfxDate;
      boolean dateOnly = datePrefix.length() <= 8;
      String iso;
      if (dateOnly) {
        iso = parsed.toInstant().atZone(ZoneOffset.UTC).toLocalDate().toString();
      } else {
        iso = ISO_INSTANT_FORMAT.format(parsed.toInstant());
      }
      return Messages.NormalizeDateResult.newBuilder().setIsoDate(iso).setError(okError()).build();
    } catch (Throwable t) {
      return Messages.NormalizeDateResult.newBuilder()
          .setError(error("MALFORMED", "Could not parse ofx_date '" + rawOfxDate + "': " + safeMessage(t)))
          .build();
    }
  }
}
