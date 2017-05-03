/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.as4.server.servlet;

import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.as4.messaging.domain.CreatePullRequestMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4XMLHelper;

public class PullRequestTest extends AbstractUserMessageTestSetUpExt
{

  @Test
  public void sendPullRequestSuccess () throws Exception
  {
    final Document aDoc = CreatePullRequestMessage.createPullRequestMessage (ESOAPVersion.AS4_DEFAULT,
                                                                             MessageHelperMethods.createEbms3MessageInfo (),
                                                                             "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC")
                                                  .getAsSOAPDocument ();
    final HttpEntity aEntity = new StringEntity (AS4XMLHelper.serializeXML (aDoc));
    sendPlainMessage (aEntity, true, null);
  }
}
