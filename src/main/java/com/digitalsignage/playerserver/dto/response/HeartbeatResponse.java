package com.digitalsignage.playerserver.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeartbeatResponse {

    private boolean success;

    @JsonProperty("next_interval_sec")
    private int nextIntervalSec;

    private List<CommandItem> commands;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getNextIntervalSec() {
        return nextIntervalSec;
    }

    public void setNextIntervalSec(int nextIntervalSec) {
        this.nextIntervalSec = nextIntervalSec;
    }

    public List<CommandItem> getCommands() {
        return commands;
    }

    public void setCommands(List<CommandItem> commands) {
        this.commands = commands;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CommandItem {

        @JsonProperty("command_id")
        private String commandId;

        private String type;

        @JsonProperty("issued_at")
        private long issuedAt;

        @JsonProperty("expire_at")
        private long expireAt;

        @JsonProperty("payload_json")
        private Object payloadJson;

        public String getCommandId() {
            return commandId;
        }

        public void setCommandId(String commandId) {
            this.commandId = commandId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public long getIssuedAt() {
            return issuedAt;
        }

        public void setIssuedAt(long issuedAt) {
            this.issuedAt = issuedAt;
        }

        public long getExpireAt() {
            return expireAt;
        }

        public void setExpireAt(long expireAt) {
            this.expireAt = expireAt;
        }

        public Object getPayloadJson() {
            return payloadJson;
        }

        public void setPayloadJson(Object payloadJson) {
            this.payloadJson = payloadJson;
        }
    }
}
