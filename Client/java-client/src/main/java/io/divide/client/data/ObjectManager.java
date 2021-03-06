/*
 * Copyright (C) 2014 Divide.io
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

package io.divide.client.data;

import com.google.inject.Inject;
import io.divide.client.BackendObject;
import io.divide.client.BackendUser;
import io.divide.shared.server.DAO;
import io.divide.shared.transitory.query.Query;
import io.divide.shared.transitory.query.SelectOperation;
import rx.Observable;

import java.util.Arrays;
import java.util.Collection;

public class ObjectManager {

    @Inject DataManager dataManager;
    @Inject DAO<BackendObject,BackendObject> localStorage;

    public ObjectManager(){ }

    public DAO<BackendObject,BackendObject> local(){
        return localStorage;
    }

    public RemoteStorage remote(){
        return remote;
    }

    private RemoteStorage remote = new RemoteStorage();
    public class RemoteStorage{

        private RemoteStorage(){};

        private <B extends BackendObject> Collection<B> assignUser(BackendUser user, B... objects) throws NotLoggedInException {
            return assignUser(user, Arrays.asList(objects));
        }

        private <B extends BackendObject> Collection<B> assignUser(BackendUser user,Collection<B> objects) throws NotLoggedInException{
            for(BackendObject o : objects){
                if(o.getOwnerId() == null){
                    o.setOwnerId(user.getOwnerId());
                }
            }
            return objects;
        }

        public <B extends BackendObject> Observable<Void> save(B... objects){
            try{
                BackendUser user;
                if((user = BackendUser.getUser()) == null) throw new NotLoggedInException();

                return dataManager.send(assignUser(user, objects));
            }catch (Exception e){
                return Observable.error(e);
            }
        }

        public <B extends BackendObject> Observable<Collection<B>> load(Class<B> type, String... keys){
            return dataManager.get(type,Arrays.asList(keys));
        }

        public <B extends BackendObject> Observable<Collection<B>> query(Class<B> type, Query query){
            if(query.getSelect() != null){
                SelectOperation so = query.getSelect();
                if(!so.getType().equals(type))
                    throw new IllegalStateException(so.getErrorMessage());
            }
            if (!query.getFrom().equals(Query.safeTable(type))){
                throw new IllegalStateException("Can not return a different type then what is queried!\n" +
                        "Expected: " + query.getFrom() + "\n" +
                        "Actual: " + Query.safeTable(type));
            }
            return dataManager.query(type,query);
        }

        public <B extends BackendObject> Observable<Integer> count(Class<B> type){
            return dataManager.count(type);
        }

    }

    public static class NotLoggedInException extends Exception {
    }
}
