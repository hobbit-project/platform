
export enum Role {
    UNAUTHORIZED,
    CHALLENGE_ORGANISER,
    SYSTEM_PROVIDER,
    GUEST
}
export function parseRole(value: string): Role {
    if (value.toLowerCase() === 'challenge-organiser')
        return Role.CHALLENGE_ORGANISER;
    if (value.toLowerCase() === 'system-provider')
        return Role.SYSTEM_PROVIDER;
    if (value.toLowerCase() === 'guest')
        return Role.GUEST;
    return Role.UNAUTHORIZED;
}

export default Role;



export class User {

    static fromJson(json: any): User {
        return new User(json.principalName, json.userName, json.name, json.email, json.roles);
    }

    public roles: Role[] = [];

    constructor(public principalName: String, public userName: String,
        public name: String, public email: String, roles: any[]) {
        for (let i = 0; i < roles.length; i++)
            this.roles.push(parseRole(roles[i]));
    }

    hasRole(role: Role): boolean {
        return this.roles.includes(role);
    }
}




export class NamedEntity {
    constructor(public id: string, public name: string, public description?: string) {
    }
}

export class SelectOption {
    constructor(public label: string, public value: string) { }
}

export class ConfigurationParameter extends NamedEntity {
    constructor(public id: string, public name: string, public datatype: string,
        public description?: string, public required?: boolean, public defaultValue?: string, public min?: number, public max?: number,
        public options?: SelectOption[], public range?: string) {
        super(id, name, description);
    }
}

export class ConfigurationParameterValue extends NamedEntity {
    constructor(public id: string, public name: string, public datatype: string,
        public value: string, public description?: string, public range?: string) {
        super(id, name, description);
    }
}

export class System extends NamedEntity {
}

export class BenchmarkOverview extends NamedEntity {

    constructor(public id: string, public name: string, protected _description?: string) {
        super(id, name, _description);
    }

    get description(): string {
        return this.description ? this.description : '';
    }

}

export class Benchmark extends BenchmarkOverview {
    constructor(public id: string, public name: string, public systems?: System[],
        public configurationParams?: ConfigurationParameter[], protected _description?: string,
        public configurationParamValues?: ConfigurationParameterValue[]) {
        super(id, name, _description);
    }
}
