package com.island.ohara.integration;

import com.island.ohara.common.util.CommonUtil;
import com.island.ohara.common.util.Releasable;
import com.island.ohara.common.util.ReleaseOnce;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ProcessShellCommandFactory;
import org.apache.sshd.server.shell.ProcessShellFactory;

public interface SshdServer extends Releasable {

  /** @return ssh server's hostname */
  String hostname();

  /** @return ssh server's port */
  int port();

  /** @return ssh client's user */
  String user();

  /** @return ssh client's password */
  String password();

  interface CommandHandler {
    /**
     * @param cmd will be executed on ssh server
     * @return true if this handler want to handle the command
     */
    boolean belong(String cmd);

    /**
     * @param cmd will be executed on ssh server
     * @return response of the command
     */
    List<String> execute(String cmd);
  }

  String SSHD_SERVER = "ohara.it.sshd";

  static SshdServer of() {
    return of(System.getenv(SSHD_SERVER));
  }

  static List<String> parseString(String sshdString) {
    return Arrays.asList(
        sshdString.split(":")[0],
        sshdString.split(":")[1].split("@")[0],
        sshdString.split(":")[1].split("@")[1],
        sshdString.split(":")[2]);
  }

  static SshdServer of(String sshdString) {
    if (sshdString == null) return local(0, Collections.emptyList());
    else {
      // format => user:password@host:port
      List<String> ss = parseString(sshdString);
      String user = ss.get(0);
      String password = ss.get(1);
      String host = ss.get(2);
      int port = Integer.parseInt(ss.get(3));
      return new SshdServer() {

        @Override
        public void close() {
          // do nothing
        }

        @Override
        public String hostname() {
          return host;
        }

        @Override
        public int port() {
          return port;
        }

        @Override
        public String user() {
          return user;
        }

        @Override
        public String password() {
          return password;
        }
      };
    }
  }

  static SshdServer local() {
    return local(0, Collections.emptyList());
  }

  static SshdServer local(int port, List<CommandHandler> handlers) {
    String _user = CommonUtil.randomString();
    String _password = CommonUtil.randomString();
    SshServer sshd = SshServer.setUpDefaultServer();
    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
    sshd.setPasswordAuthenticator(
        (String username, String password, ServerSession session) ->
            username.equals(_user) && password.equals(_password));
    sshd.setShellFactory(new ProcessShellFactory(Arrays.asList("/bin/sh", "-i", "-l")));
    sshd.setCommandFactory(
        (String command) ->
            handlers
                .stream()
                .filter(h -> h.belong(command))
                .findFirst()
                .map(
                    h ->
                        (Command)
                            new Command() {
                              private OutputStream out = null;
                              private OutputStream err = null;
                              private ExitCallback callback = null;

                              @Override
                              public void start(Environment env) {
                                try {
                                  h.execute(command)
                                      .forEach(
                                          s -> {
                                            try {
                                              out.write(s.getBytes());
                                              // TODO: make it configurable...by chia
                                              out.write("\n".getBytes());
                                            } catch (Throwable e) {
                                              throw new RuntimeException(e);
                                            }
                                          });
                                  callback.onExit(0);
                                } catch (Throwable e) {
                                  callback.onExit(2, e.getMessage());
                                }
                              }

                              @Override
                              public void destroy() {
                                ReleaseOnce.close(out);
                                ReleaseOnce.close(err);
                              }

                              @Override
                              public void setInputStream(InputStream in) {
                                // do nothing
                              }

                              @Override
                              public void setOutputStream(OutputStream out) {
                                this.out = out;
                              }

                              @Override
                              public void setErrorStream(OutputStream err) {
                                this.err = err;
                              }

                              @Override
                              public void setExitCallback(ExitCallback callback) {
                                this.callback = callback;
                              }
                            })
                .orElseGet(() -> ProcessShellCommandFactory.INSTANCE.createCommand(command)));
    sshd.setHost(CommonUtil.hostname());
    sshd.setPort(Math.max(port, 0));
    try {
      sshd.start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new SshdServer() {

      @Override
      public void close() {
        ReleaseOnce.close(sshd);
      }

      @Override
      public String hostname() {
        return sshd.getHost();
      }

      @Override
      public int port() {
        return sshd.getPort();
      }

      @Override
      public String user() {
        return _user;
      }

      @Override
      public String password() {
        return _password;
      }
    };
  }
}