/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2013 NAVER Corp.
 * http://yobi.io
 *
 * @Author Yi EungJun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package models;

import models.enumeration.State;
import play.data.format.Formats;
import play.data.validation.Constraints;

import java.util.Date;
import java.util.List;

public class IssueMassUpdate {
    public State state;
    public User assignee;
    public Milestone milestone;
    public boolean delete;
    @Formats.DateTime(pattern = "yyyy-MM-dd")
    public Date dueDate;
    public boolean isDueDateChanged;

    @Constraints.Required
    public List<Issue> issues;
    public List<IssueLabel> attachingLabel;
    public List<IssueLabel> detachingLabel;
}
