
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

