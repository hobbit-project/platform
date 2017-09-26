import { Type } from 'class-transformer';


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

    constructor(public principalName: string, public userName: string,
        public name: string, public email: string, roles: any[]) {
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


export class ConfigParam extends NamedEntity {
    constructor(id: string, name: string, public datatype: string,
        description?: string, public range?: string) {
        super(id, name, description);
    }

    isText(): boolean {
        return this.datatype === 'xsd:string';
    }

    isFloatingPointNumber(): boolean {
        return this.datatype === 'xsd:decimal' || this.datatype === 'xsd:float' || this.datatype === 'xsd:double';
    }

    isNumber(): boolean {
        return this.datatype === 'xsd:integer' || this.isFloatingPointNumber() || this.datatype === 'xsd:unsignedInt';
    }

    isBoolean(): boolean {
        return this.datatype === 'xsd:boolean';
    }

    getInputType(): string {
        if (this.isNumber())
            return 'number';
        if (this.isText())
            return 'text';
        return undefined;
    }
}

export class ConfigParamDefinition extends ConfigParam {

    @Type(() => SelectOption)
    public options?: SelectOption[];

    constructor(id: string, name: string, datatype: string, description?: string, range?: string,
        public required?: boolean, public defaultValue?: string,
        public min?: number, public max?: number) {
        super(id, name, datatype, description, range);
    }

    getType() {
        if (this.getInputType() !== undefined)
            return 'input';
        if (this.options !== undefined)
            return 'dropdown';
        if (this.isBoolean())
            return 'boolean';
        return '';
    }

}

export class ConfigParamRealisation extends ConfigParam {
    constructor(id: string, name: string, datatype: string,
        public value: string, description?: string, range?: string) {
        super(id, name, datatype, description, range);
    }
}

export class System extends NamedEntity {
}

export class BenchmarkOverview extends NamedEntity {

    constructor(id: string, name: string, description?: string) {
        super(id, name, description);
    }


}

export class Benchmark extends BenchmarkOverview {

    @Type(() => ConfigParamDefinition)
    public configurationParams?: ConfigParamDefinition[];

    @Type(() => ConfigParamRealisation)
    public configParamValues?: ConfigParamRealisation[];

    @Type(() => System)
    public systems?: System[];

    constructor(id: string, name: string, description?: string) {
        super(id, name, description);
    }

    hasConfigParams() {
        return this.configurationParams;
    }
}



export class ChallengeTask extends NamedEntity {

    @Type(() => ConfigParamRealisation)
    public configurationParams?: ConfigParamRealisation[];

    @Type(() => Benchmark)
    public benchmark: Benchmark;

    constructor(id: string, name: string, description?: string) {
        super(id, name, description);
    }
}

export class Challenge extends NamedEntity {

    @Type(() => ChallengeTask)
    public tasks: ChallengeTask[] = [];

    constructor(id: string, name: string, description?: string,
        public organizer?: string,
        public executionDate?: string, public publishDate?: string,
        public visible?: boolean, public closed?: boolean) {
        super(id, name, description);
    }
}
